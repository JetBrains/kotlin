/*
 * Copyright 2010-2015 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.jvm.runtime

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.cli.common.output.outputUtils.writeAllTo
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.load.kotlin.reflect.RuntimeModuleData
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.resolve.scopes.*
import org.jetbrains.kotlin.serialization.deserialization.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.JetTestUtils.TestFileFactoryNoModules
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.Configuration
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.net.URLClassLoader
import java.util.regex.Pattern

public abstract class AbstractJvmRuntimeDescriptorLoaderTest : TestCaseWithTmpdir() {
    companion object {
        private val renderer = DescriptorRenderer.withOptions {
            withDefinedIn = false
            excludedAnnotationClasses = (listOf(
                    FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)
            ) + JvmAnnotationNames.ANNOTATIONS_COPIED_TO_TYPES).toSet()
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            includePropertyConstant = false
            verbose = true
            renderDefaultAnnotationArguments = true
            modifiers = DescriptorRendererModifier.ALL
        }
    }

    // NOTE: this test does a dirty hack of text substitution to make all annotations defined in source code retain at runtime.
    // Specifically each @interface in Java sources is extended by @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    // Also type related annotations are removed from Java because they are invisible at runtime
    protected fun doTest(fileName: String) {
        val file = File(fileName)
        val text = FileUtil.loadFile(file, true)

        if (InTextDirectivesUtils.isDirectiveDefined(text, "SKIP_IN_RUNTIME_TEST")) return

        val jdkKind =
                if (InTextDirectivesUtils.isDirectiveDefined(text, "FULL_JDK")) TestJdkKind.FULL_JDK
                else TestJdkKind.MOCK_JDK

        compileFile(file, text, jdkKind)

        val classLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeAndReflectJarClassLoader())

        val actual = createReflectedPackageView(classLoader, JvmResolveUtil.TEST_MODULE_NAME)

        val expected = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), jdkKind, ConfigurationKind.ALL, true
        ).first

        val comparatorConfiguration = Configuration(
                /* checkPrimaryConstructors = */ fileName.endsWith(".kt"),
                /* checkPropertyAccessors = */ true,
                /* includeMethodsOfKotlinAny = */ false,
                // Skip Java annotation constructors because order of their parameters is not retained at runtime
                { descriptor -> !descriptor.isJavaAnnotationConstructor() },
                errorTypesForbidden(), renderer
        )
        RecursiveDescriptorComparator.validateAndCompareDescriptors(expected, actual, comparatorConfiguration, null)
    }

    private fun DeclarationDescriptor.isJavaAnnotationConstructor() =
            this is ConstructorDescriptor &&
            getContainingDeclaration().let { container ->
                container is JavaClassDescriptor &&
                container.getKind() == ClassKind.ANNOTATION_CLASS
            }

    private fun compileFile(file: File, text: String, jdkKind: TestJdkKind) {
        val fileName = file.getName()
        when {
            fileName.endsWith(".java") -> {
                val sources = JetTestUtils.createTestFiles(fileName, text, object : TestFileFactoryNoModules<File>() {
                    override fun create(fileName: String, text: String, directives: Map<String, String>): File {
                        val targetFile = File(tmpdir, fileName)
                        targetFile.writeText(adaptJavaSource(text))
                        return targetFile
                    }
                })
                LoadDescriptorUtil.compileJavaWithAnnotationsJar(sources, tmpdir)
            }
            fileName.endsWith(".kt") -> {
                val environment = JetTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                        myTestRootDisposable, ConfigurationKind.ALL, jdkKind
                )
                val jetFile = JetTestUtils.createFile(file.getPath(), text, environment.project)
                GenerationUtils.compileFileGetClassFileFactoryForTest(jetFile, environment).writeAllTo(tmpdir)
            }
        }
    }

    private fun createReflectedPackageView(classLoader: URLClassLoader, moduleName: String): SyntheticPackageViewForTest {
        val moduleData = RuntimeModuleData.create(classLoader)
        moduleData.packageFacadeProvider.registerModule(moduleName)
        val module = moduleData.module


        val generatedPackageDir = File(tmpdir, LoadDescriptorUtil.TEST_PACKAGE_FQNAME.pathSegments().single().asString())
        val allClassFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.class"), generatedPackageDir)

        val packageScopes = arrayListOf<KtScope>()
        val classes = arrayListOf<ClassDescriptor>()
        var shouldAddPackageView = false
        for (classFile in allClassFiles) {
            val className = classFile.relativeTo(tmpdir).substringBeforeLast(".class").replace('/', '.').replace('\\', '.')

            val klass = classLoader.loadClass(className).sure { "Couldn't load class $className" }
            val header = ReflectKotlinClass.create(klass)?.getClassHeader()

            if (header?.kind == KotlinClassHeader.Kind.PACKAGE_FACADE ||
                header?.kind == KotlinClassHeader.Kind.FILE_FACADE ||
                header?.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS
            ) {
                val packageView = module.getPackage(LoadDescriptorUtil.TEST_PACKAGE_FQNAME)
                if (!packageScopes.contains(packageView.memberScope)) {
                    packageScopes.add(packageView.memberScope)
                }
            }
            else if (header == null || (header.kind == KotlinClassHeader.Kind.CLASS && !header.isLocalClass)) {
                // Either a normal Kotlin class or a Java class
                val classId = klass.classId
                if (!classId.isLocal) {
                    val classDescriptor = module.findClassAcrossModuleDependencies(classId).sure { "Couldn't resolve class $className" }
                    if (DescriptorUtils.isTopLevelDeclaration(classDescriptor)) {
                        classes.add(classDescriptor)
                    }
                }
            }
        }

        // Since runtime package view descriptor doesn't support getAllDescriptors(), we construct a synthetic package view here.
        // It has in its scope descriptors for all the classes and top level members generated by the compiler
        return SyntheticPackageViewForTest(module, packageScopes, classes)
    }

    private fun adaptJavaSource(text: String): String {
        val typeAnnotations = arrayOf("NotNull", "Nullable", "ReadOnly", "Mutable")
        return typeAnnotations.fold(text) { text, annotation -> text.replace("@$annotation", "") }.replace(
                "@interface",
                "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @interface"
        )
    }

    private class SyntheticPackageViewForTest(override val module: ModuleDescriptor,
                                              packageScopes: List<KtScope>,
                                              classes: List<ClassifierDescriptor>) : PackageViewDescriptor {
        private val scope: KtScope

        init {
            val writableScope = WritableScopeImpl(KtScope.Empty, this, RedeclarationHandler.THROW_EXCEPTION, "runtime descriptor loader test")
            classes.forEach { writableScope.addClassifierDescriptor(it) }
            writableScope.changeLockLevel(WritableScope.LockLevel.READING)
            scope = ChainedScope(this, "synthetic package view for test", writableScope, *packageScopes.toTypedArray())
        }

        override val fqName: FqName
            get() = LoadDescriptorUtil.TEST_PACKAGE_FQNAME
        override val memberScope: KtScope
            get() = scope
        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
                visitor.visitPackageViewDescriptor(this, data)

        override fun getContainingDeclaration() = null
        override fun getOriginal() = throw UnsupportedOperationException()
        override fun substitute(substitutor: TypeSubstitutor) = throw UnsupportedOperationException()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = throw UnsupportedOperationException()
        override fun getAnnotations() = throw UnsupportedOperationException()
        override fun getName() = throw UnsupportedOperationException()
        override val fragments: Nothing
            get() = throw UnsupportedOperationException()
    }
}
