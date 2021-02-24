/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.runtime

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.checkers.KotlinMultiFileTestWithJava
import org.jetbrains.kotlin.cli.common.CLIConfigurationKeys
import org.jetbrains.kotlin.codegen.GenerationUtils
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil
import org.jetbrains.kotlin.jvm.compiler.LoadDescriptorUtil
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.test.*
import org.jetbrains.kotlin.test.TestFiles.TestFileFactoryNoModules
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden
import org.jetbrains.kotlin.test.util.KtTestUtil
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.Configuration
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.net.URLClassLoader
import java.util.*
import java.util.regex.Pattern

abstract class AbstractJvmRuntimeDescriptorLoaderTest : TestCaseWithTmpdir() {
    companion object {
        private val renderer = DescriptorRenderer.withOptions {
            withDefinedIn = false
            excludedAnnotationClasses = setOf(
                FqName(ExpectedLoadErrorsUtil.ANNOTATION_CLASS_NAME)
            )
            overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            includePropertyConstant = false
            verbose = true
            annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
            renderDefaultAnnotationArguments = true
            modifiers = DescriptorRendererModifier.ALL
        }
    }

    protected open val defaultJdkKind: TestJdkKind = TestJdkKind.MOCK_JDK

    // NOTE: this test does a dirty hack of text substitution to make all annotations defined in source code retain at runtime.
    // Specifically each @interface in Java sources is extended by @java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    // Also type related annotations are removed from Java because they are invisible at runtime
    protected fun doTest(fileName: String) {
        val file = File(fileName)
        val text = FileUtil.loadFile(file, true)

        if (InTextDirectivesUtils.isDirectiveDefined(text, "SKIP_IN_RUNTIME_TEST")) return

        val jdkKind =
            if (InTextDirectivesUtils.isDirectiveDefined(text, "FULL_JDK")) TestJdkKind.FULL_JDK
            else defaultJdkKind

        compileFile(file, text, jdkKind)

        val classLoader = URLClassLoader(arrayOf(tmpdir.toURI().toURL()), ForTestCompileRuntime.runtimeAndReflectJarClassLoader())

        val actual = createReflectedPackageView(classLoader)

        val comparatorConfiguration = Configuration(
            /* checkPrimaryConstructors = */ fileName.endsWith(".kt"),
            /* checkPropertyAccessors = */ true,
            /* includeMethodsOfKotlinAny = */ false,
            /* renderDeclarationsFromOtherModules = */ true,
            /* checkFunctionContract = */ false,
            // Skip Java annotation constructors because order of their parameters is not retained at runtime
            { descriptor -> !descriptor!!.isJavaAnnotationConstructor() },
            errorTypesForbidden(), renderer
        )

        val differentResultFile = KotlinTestUtils.replaceExtension(file, "runtime.txt")
        if (differentResultFile.exists()) {
            RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile(actual, comparatorConfiguration, differentResultFile)
            return
        }

        val expected = LoadDescriptorUtil.loadTestPackageAndBindingContextFromJavaRoot(
            tmpdir, testRootDisposable, jdkKind, ConfigurationKind.ALL, true, false, false, false, null
        ).first

        RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptors(expected, actual, comparatorConfiguration, null)
    }

    private fun DeclarationDescriptor.isJavaAnnotationConstructor() =
        this is ClassConstructorDescriptor &&
                containingDeclaration is JavaClassDescriptor &&
                containingDeclaration.kind == ClassKind.ANNOTATION_CLASS

    private fun compileFile(file: File, text: String, jdkKind: TestJdkKind) {
        val fileName = file.name
        when {
            fileName.endsWith(".java") -> {
                val sources = TestFiles.createTestFiles(
                    fileName,
                    text,
                    object : TestFileFactoryNoModules<File>() {
                        override fun create(fileName: String, text: String, directives: Directives): File {
                            val targetFile = File(tmpdir, fileName)
                            targetFile.writeText(adaptJavaSource(text))
                            return targetFile
                        }
                    }
                )
                LoadDescriptorUtil.compileJavaWithAnnotationsJar(sources, tmpdir, emptyList(), null, false)
            }
            fileName.endsWith(".kt") -> {
                val environment = KotlinTestUtils.createEnvironmentWithJdkAndNullabilityAnnotationsFromIdea(
                    testRootDisposable, ConfigurationKind.ALL, jdkKind
                )
                for (root in environment.configuration.getList(CLIConfigurationKeys.CONTENT_ROOTS)) {
                    LOG.info("root: $root")
                }
                val ktFile = KtTestUtil.createFile(file.path, text, environment.project)
                GenerationUtils.compileFileTo(ktFile, environment, tmpdir)
            }
        }
    }

    private fun createReflectedPackageView(classLoader: URLClassLoader): SyntheticPackageViewForTest {
        val moduleData = RuntimeModuleData.create(classLoader)
        val module = moduleData.module

        val generatedPackageDir = File(tmpdir, LoadDescriptorUtil.TEST_PACKAGE_FQNAME.pathSegments().single().asString())
        val allClassFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.class"), generatedPackageDir)

        val packageScopes = arrayListOf<MemberScope>()
        val classes = arrayListOf<ClassDescriptor>()
        for (classFile in allClassFiles) {
            val className = classFile.toRelativeString(tmpdir).substringBeforeLast(".class").replace('/', '.').replace('\\', '.')

            val klass = classLoader.loadClass(className).sure { "Couldn't load class $className" }
            val binaryClass = ReflectKotlinClass.create(klass)
            val header = binaryClass?.classHeader

            if (header?.kind == KotlinClassHeader.Kind.FILE_FACADE || header?.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS) {
                packageScopes.add(moduleData.packagePartScopeCache.getPackagePartScope(binaryClass))
            } else if (header == null || header.kind == KotlinClassHeader.Kind.CLASS) {
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
        val adaptedSource = typeAnnotations.fold(text) { result, annotation -> result.replace("@$annotation", "") }
        if ("@Retention" !in adaptedSource) {
            return adaptedSource.replace(
                "@interface",
                "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @interface"
            )
        }
        return adaptedSource
    }

    private class SyntheticPackageViewForTest(
        override val module: ModuleDescriptor,
        packageScopes: List<MemberScope>,
        classes: List<ClassifierDescriptor>
    ) : PackageViewDescriptor {
        private val scope: MemberScope

        init {
            val list = ArrayList<MemberScope>(packageScopes.size + 1)
            list.add(ScopeWithClassifiers(classes))
            list.addAll(packageScopes)
            scope = ChainedMemberScope.create("synthetic package view for test", list)
        }

        override val fqName: FqName
            get() = LoadDescriptorUtil.TEST_PACKAGE_FQNAME
        override val memberScope: MemberScope
            get() = scope
        override val fragments: List<PackageFragmentDescriptor> = listOf(
            object : PackageFragmentDescriptorImpl(module, fqName) {
                override fun getMemberScope(): MemberScope = scope
            }
        )

        override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D): R =
            visitor.visitPackageViewDescriptor(this, data)

        override fun getContainingDeclaration(): PackageViewDescriptor? = null
        override fun getOriginal() = throw UnsupportedOperationException()
        override fun acceptVoid(visitor: DeclarationDescriptorVisitor<Void, Void>?) = throw UnsupportedOperationException()
        override fun getName() = throw UnsupportedOperationException()
        override val annotations: Annotations
            get() = throw UnsupportedOperationException()
    }

    private class ScopeWithClassifiers(classifiers: List<ClassifierDescriptor>) : MemberScopeImpl() {
        private val classifierMap = HashMap<Name, ClassifierDescriptor>()

        init {
            for (classifier in classifiers) {
                classifierMap.put(classifier.name, classifier)?.let {
                    throw IllegalStateException(
                        String.format(
                            "Redeclaration: %s (%s) and %s (%s) (no line info available)",
                            DescriptorUtils.getFqName(it), it,
                            DescriptorUtils.getFqName(classifier), classifier
                        )
                    )
                }
            }
        }

        override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor? = classifierMap[name]

        override fun getContributedDescriptors(
            kindFilter: DescriptorKindFilter,
            nameFilter: (Name) -> Boolean
        ): Collection<DeclarationDescriptor> = classifierMap.values

        override fun printScopeStructure(p: Printer) {
            p.println("runtime descriptor loader test")
        }
    }

}

private val LOG = Logger.getInstance(KotlinMultiFileTestWithJava::class.java)
