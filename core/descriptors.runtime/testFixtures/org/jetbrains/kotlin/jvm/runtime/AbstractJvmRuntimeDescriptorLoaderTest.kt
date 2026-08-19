/*
 * Copyright 2010-2025 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.jvm.runtime

import com.intellij.openapi.util.io.FileUtil
import org.jetbrains.kotlin.codegen.forTestCompile.ForTestCompileRuntime
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.descriptors.runtime.components.ReflectKotlinClass
import org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData
import org.jetbrains.kotlin.descriptors.runtime.structure.classId
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.jvm.compiler.ExpectedLoadErrorsUtil
import org.jetbrains.kotlin.load.java.descriptors.JavaClassDescriptor
import org.jetbrains.kotlin.load.kotlin.header.KotlinClassHeader
import org.jetbrains.kotlin.name.ClassIdBasedLocality
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.renderer.*
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.ChainedMemberScope
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.test.FirParser
import org.jetbrains.kotlin.test.backend.handlers.JvmBinaryArtifactHandler
import org.jetbrains.kotlin.test.builders.TestConfigurationBuilder
import org.jetbrains.kotlin.test.builders.configureJvmArtifactsHandlersStep
import org.jetbrains.kotlin.test.configuration.setupJvmPipelineSteps
import org.jetbrains.kotlin.test.directives.ConfigurationDirectives
import org.jetbrains.kotlin.test.directives.JvmEnvironmentConfigurationDirectives
import org.jetbrains.kotlin.test.directives.model.SimpleDirectivesContainer
import org.jetbrains.kotlin.test.model.BinaryArtifacts
import org.jetbrains.kotlin.test.model.TestFailureSuppressorBySingleDirective
import org.jetbrains.kotlin.test.model.TestFile
import org.jetbrains.kotlin.test.model.TestModule
import org.jetbrains.kotlin.test.runners.AbstractKotlinCompilerJvmTest
import org.jetbrains.kotlin.test.services.SourceFilePreprocessor
import org.jetbrains.kotlin.test.services.TestServices
import org.jetbrains.kotlin.test.services.isJavaFile
import org.jetbrains.kotlin.test.services.jvm.compiledClassesManager
import org.jetbrains.kotlin.test.services.moduleStructure
import org.jetbrains.kotlin.test.util.DescriptorValidator.ValidationVisitor.errorTypesForbidden
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator.Configuration
import org.jetbrains.kotlin.test.utils.withExtension
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.sure
import java.io.File
import java.net.URLClassLoader
import java.util.regex.Pattern

/**
 * Verifies that the runtime, reflection-based descriptor loader ([org.jetbrains.kotlin.descriptors.runtime.components.RuntimeModuleData])
 * produces descriptors matching the ones produced by K1 source analysis (or a golden `*.runtime.txt` file).
 */
abstract class AbstractJvmRuntimeDescriptorLoaderTest : AbstractKotlinCompilerJvmTest() {
    override fun configure(builder: TestConfigurationBuilder): Unit = with(builder) {
        setupJvmPipelineSteps(FirParser.LightTree)

        defaultDirectives {
            +ConfigurationDirectives.WITH_STDLIB
            +JvmEnvironmentConfigurationDirectives.WITH_REFLECT
        }

        useSourcePreprocessor(::MakeJavaAnnotationsRuntimeRetained)
        useFailureSuppressors(::RuntimeDescriptorLoaderTestSuppressor)

        configureJvmArtifactsHandlersStep {
            useHandlers(::RuntimeDescriptorLoaderHandler)
        }
    }
}

private class RuntimeDescriptorLoaderHandler(testServices: TestServices) : JvmBinaryArtifactHandler(testServices) {
    override fun processModule(module: TestModule, info: BinaryArtifacts.Jvm) {
        checkArtifact(info)

        // Flush the compiled Kotlin classes to disk. If there are Java classes, they're already compiled there by JavaCompilerFacade.
        val outputDir = testServices.compiledClassesManager.compileKotlinToDiskAndGetOutputDir(module, info.classFileFactory)

        val testDataFile = testServices.moduleStructure.originalTestDataFiles.first()

        val classLoader = URLClassLoader(arrayOf(outputDir.toURI().toURL()), ForTestCompileRuntime.runtimeAndReflectJarClassLoader())

        val actual = createReflectedPackageView(classLoader, outputDir)

        val comparatorConfiguration = Configuration(
            /* checkPrimaryConstructors = */ testDataFile.name.endsWith(".kt"),
            /* checkPropertyAccessors = */ true,
            /* includeMethodsOfKotlinAny = */ false,
            /* renderDeclarationsFromOtherModules = */ true,
            /* checkFunctionContracts = */ false,
            // Skip Java annotation constructors because order of their parameters is not retained at runtime
            { descriptor -> !descriptor!!.isJavaAnnotationConstructor() },
            errorTypesForbidden(), renderer
        )

        val expectedFile = testDataFile.withExtension("runtime.txt")
        RecursiveDescriptorComparator.validateAndCompareDescriptorWithFile(actual, comparatorConfiguration, expectedFile, assertions)
    }

    override fun processAfterAllModules(someAssertionWasFailed: Boolean) {}

    private fun DeclarationDescriptor.isJavaAnnotationConstructor() =
        this is ClassConstructorDescriptor &&
                containingDeclaration is JavaClassDescriptor &&
                containingDeclaration.kind == ClassKind.ANNOTATION_CLASS

    private fun createReflectedPackageView(classLoader: URLClassLoader, outputDir: File): SyntheticPackageViewForTest {
        val moduleData = RuntimeModuleData.create(classLoader)
        val module = moduleData.module

        val generatedPackageDir = File(outputDir, "test")
        val allClassFiles = FileUtil.findFilesByMask(Pattern.compile(".*\\.class"), generatedPackageDir)

        val packageScopes = arrayListOf<MemberScope>()
        val classes = arrayListOf<ClassDescriptor>()
        for (classFile in allClassFiles) {
            val className = classFile.toRelativeString(outputDir).substringBeforeLast(".class").replace('/', '.').replace('\\', '.')

            val klass = classLoader.loadClass(className).sure { "Couldn't load class $className" }
            val binaryClass = ReflectKotlinClass.create(klass)
            val header = binaryClass?.classHeader

            if (header?.kind == KotlinClassHeader.Kind.FILE_FACADE || header?.kind == KotlinClassHeader.Kind.MULTIFILE_CLASS) {
                packageScopes.add(moduleData.packagePartScopeCache.getPackagePartScope(binaryClass))
            } else if (header == null || header.kind == KotlinClassHeader.Kind.CLASS) {
                // Either a normal Kotlin class or a Java class
                val classId = klass.classId
                @OptIn(ClassIdBasedLocality::class)
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

        override val fqName: FqName = FqName("test")
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
}

private class RuntimeDescriptorLoaderTestSuppressor(testServices: TestServices) : TestFailureSuppressorBySingleDirective(
    RuntimeDescriptorLoaderDirectives.SKIP_IN_RUNTIME_TEST,
    RuntimeDescriptorLoaderDirectives,
    testServices,
)

private object RuntimeDescriptorLoaderDirectives : SimpleDirectivesContainer() {
    val SKIP_IN_RUNTIME_TEST by directive("Skip this test in the runtime reflection descriptor loader test")
}

private class MakeJavaAnnotationsRuntimeRetained(testServices: TestServices) : SourceFilePreprocessor(testServices) {
    override fun process(file: TestFile, content: String): String {
        if (!file.isJavaFile) return content

        val typeAnnotations = arrayOf("NotNull", "Nullable", "ReadOnly", "Mutable")
        val adaptedSource = typeAnnotations.fold(content) { result, annotation -> result.replace("@$annotation", "") }
        if ("@Retention" !in adaptedSource) {
            return adaptedSource.replace(
                "@interface",
                "@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME) @interface"
            )
        }
        return adaptedSource
    }
}
