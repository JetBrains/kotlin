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
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptorVisitor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.JvmAnnotationNames
import org.jetbrains.kotlin.load.java.structure.reflect.classId
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.load.kotlin.reflect.ReflectKotlinClass
import org.jetbrains.kotlin.load.kotlin.reflect.RuntimeModuleData
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererBuilder
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.JetScope
import org.jetbrains.kotlin.resolve.scopes.RedeclarationHandler
import org.jetbrains.kotlin.resolve.scopes.WritableScope
import org.jetbrains.kotlin.resolve.scopes.WritableScopeImpl
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
    class object {
        private val renderer = DescriptorRendererBuilder()
                .setWithDefinedIn(false)
                .setExcludedAnnotationClasses(listOf(
                        ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME,
                        // TODO: add these annotations when they are retained at runtime
                        "kotlin.deprecated",
                        "kotlin.data",
                        "kotlin.inline",
                        "org.jetbrains.annotations.NotNull",
                        "org.jetbrains.annotations.Nullable",
                        "org.jetbrains.annotations.Mutable",
                        "org.jetbrains.annotations.ReadOnly"
                ).map { FqName(it) })
                .setOverrideRenderingPolicy(DescriptorRenderer.OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE)
                .setIncludeSynthesizedParameterNames(false)
                .setIncludePropertyConstant(false)
                .setVerbose(true)
                .build()
    }

    // NOTE: this test does a dirty hack of text substitution to make all annotations defined in source code retain at runtime.
    // Specifically each "annotation class" in Kotlin sources is replaced by "Retention(RUNTIME) annotation class", and the same in Java
    protected fun doTest(fileName: String) {
        val file = File(fileName)
        val text = FileUtil.loadFile(file, true)

        if (InTextDirectivesUtils.isDirectiveDefined(text, "SKIP_IN_RUNTIME_TEST")) return

        compileFile(file, text)

        val classLoader = URLClassLoader(array(tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeJarClassLoader())

        val actual = createReflectedPackageView(classLoader)

        val expected = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
                tmpdir, getTestRootDisposable(), TestJdkKind.FULL_JDK, ConfigurationKind.ALL
        ).first

        val comparatorConfiguration = Configuration(
                /* checkPrimaryConstructors = */ fileName.endsWith(".kt"),
                /* checkPropertyAccessors = */ true,
                /* includeMethodsOfKotlinAny = */ false,
                { descriptor ->
                    // Skip annotation constructors because order of their parameters is not retained at runtime
                    !(descriptor is ConstructorDescriptor && DescriptorUtils.isAnnotationClass(descriptor.getContainingDeclaration()))
                },
                errorTypesForbidden(), renderer
        )
        RecursiveDescriptorComparator.validateAndCompareDescriptors(expected, actual, comparatorConfiguration, null)
    }

    private fun compileFile(file: File, text: String) {
        val fileName = file.getName()
        when {
            fileName.endsWith(".java") -> {
                val sources = JetTestUtils.createTestFiles(fileName, text, object : TestFileFactoryNoModules<File>() {
                    override fun create(fileName: String, text: String, directives: Map<String, String>): File {
                        val targetFile = File(tmpdir, fileName)
                        targetFile.writeText(addRuntimeRetentionToJavaSource(text))
                        return targetFile
                    }
                })
                LoadDescriptorUtil.compileJavaWithAnnotationsJar(sources, tmpdir)
            }
            fileName.endsWith(".kt") -> {
                val environment = JetTestUtils.createEnvironmentWithFullJdk(myTestRootDisposable)
                val jetFile = JetTestUtils.createFile(file.getPath(), addRuntimeRetentionToKotlinSource(text), environment.getProject())
                GenerationUtils.compileFileGetClassFileFactoryForTest(jetFile).writeAllTo(tmpdir)
            }
        }
    }

    private fun createReflectedPackageView(classLoader: URLClassLoader): SyntheticPackageViewForTest {
        val module = RuntimeModuleData.create(classLoader).module

        // Since runtime package view descriptor doesn't support getAllDescriptors(), we construct a synthetic package view here.
        // It has in its scope descriptors for all the classes and top level members generated by the compiler
        val actual = SyntheticPackageViewForTest(module)
        val scope = actual.getMemberScope()

        val generatedPackageDir = File(tmpdir, LoadDescriptorUtil.TEST_PACKAGE_FQNAME.pathSegments().single().asString())
        val allClassFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.class"), generatedPackageDir)

        for (classFile in allClassFiles) {
            val className = tmpdir.relativePath(classFile).substringBeforeLast(".class").replace('/', '.').replace('\\', '.')

            val klass = classLoader.loadClass(className).sure("Couldn't load class $className")
            val header = ReflectKotlinClass.create(klass)?.getClassHeader()

            if (header?.kind == KotlinClassHeader.Kind.PACKAGE_FACADE) {
                val packageView = module.getPackage(actual.getFqName()).sure("Couldn't resolve package ${actual.getFqName()}")
                scope.importScope(packageView.getMemberScope())
            }
            else if (header == null ||
                     (header.kind == KotlinClassHeader.Kind.CLASS && header.classKind == JvmAnnotationNames.KotlinClass.Kind.CLASS)) {
                // Either a normal Kotlin class or a Java class
                val classId = klass.classId
                if (!classId.isLocal()) {
                    val classDescriptor = module.findClassAcrossModuleDependencies(classId).sure("Couldn't resolve class $className")
                    if (DescriptorUtils.isTopLevelDeclaration(classDescriptor)) {
                        scope.addClassifierDescriptor(classDescriptor)
                    }
                }
            }
        }
        return actual
    }

    private fun addRuntimeRetentionToKotlinSource(text: String): String {
        return text.replace(
                "annotation class",
                "[java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)] annotation class"
        )
    }

    private fun addRuntimeRetentionToJavaSource(text: String): String {
        return text.replace(
                "@interface",
                "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @interface"
        )
    }

    private class SyntheticPackageViewForTest(private val module: ModuleDescriptor) : PackageViewDescriptor {
        private val scope = WritableScopeImpl(JetScope.Empty, this, RedeclarationHandler.THROW_EXCEPTION, "runtime descriptor loader test")

        ;{
            scope.changeLockLevel(WritableScope.LockLevel.BOTH)
        }

        override fun getFqName() = LoadDescriptorUtil.TEST_PACKAGE_FQNAME
        override fun getMemberScope() = scope
        override fun getModule() = module
        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
                visitor.visitPackageViewDescriptor(this, data)

        override fun getContainingDeclaration() = throw UnsupportedOperationException()
        override fun getOriginal() = throw UnsupportedOperationException()
        override fun substitute(substitutor: TypeSubstitutor) = throw UnsupportedOperationException()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = throw UnsupportedOperationException()
        override fun getAnnotations() = throw UnsupportedOperationException()
        override fun getName() = throw UnsupportedOperationException()
    }
}
