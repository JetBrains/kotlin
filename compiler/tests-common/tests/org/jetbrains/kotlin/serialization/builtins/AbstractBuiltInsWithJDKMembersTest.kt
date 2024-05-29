/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.serialization.builtins

import org.jetbrains.kotlin.builtins.BuiltInsPackageFragment
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.builtins.StandardNames.BUILT_INS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.COLLECTIONS_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.builtins.StandardNames.RANGES_PACKAGE_FQ_NAME
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.impl.ModuleDescriptorImpl
import org.jetbrains.kotlin.descriptors.packageFragments
import org.jetbrains.kotlin.renderer.AnnotationArgumentsRenderingPolicy
import org.jetbrains.kotlin.renderer.DescriptorRenderer
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier
import org.jetbrains.kotlin.renderer.OverrideRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.isSourceAnnotation
import org.jetbrains.kotlin.resolve.lazy.JvmResolveUtil
import org.jetbrains.kotlin.test.ConfigurationKind
import org.jetbrains.kotlin.test.KotlinTestWithEnvironment
import org.jetbrains.kotlin.test.TestJdkKind
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparator
import org.jetbrains.kotlin.test.util.RecursiveDescriptorComparatorAdaptor
import org.jetbrains.kotlin.types.isError
import java.io.File

abstract class AbstractBuiltInsWithJDKMembersTest : KotlinTestWithEnvironment() {
    private val configuration = createComparatorConfiguration()

    override fun createEnvironment(): KotlinCoreEnvironment =
        createEnvironmentWithJdk(ConfigurationKind.JDK_ONLY, TestJdkKind.FULL_JDK)

    protected fun doTest(builtinVersionName: String) {
        val module = JvmResolveUtil.analyze(environment).moduleDescriptor as ModuleDescriptorImpl

        for (packageFqName in listOf(BUILT_INS_PACKAGE_FQ_NAME, COLLECTIONS_PACKAGE_FQ_NAME, RANGES_PACKAGE_FQ_NAME)) {
            val loaded = module.packageFragmentProvider.packageFragments(packageFqName)
                .filterIsInstance<BuiltInsPackageFragment>()
                .single { !it.isFallback }
            RecursiveDescriptorComparatorAdaptor.validateAndCompareDescriptorWithFile(
                loaded, configuration,
                File("compiler/testData/builtin-classes/$builtinVersionName/" + packageFqName.asString().replace('.', '-') + ".txt")
            )
        }
    }

    companion object {
        @JvmStatic
        fun createComparatorConfiguration(): RecursiveDescriptorComparator.Configuration {
            return RecursiveDescriptorComparator.RECURSIVE_ALL.includeMethodsOfKotlinAny(false).withRenderer(
                DescriptorRenderer.withOptions {
                    withDefinedIn = false
                    overrideRenderingPolicy = OverrideRenderingPolicy.RENDER_OPEN_OVERRIDE
                    verbose = true
                    annotationArgumentsRenderingPolicy = AnnotationArgumentsRenderingPolicy.UNLESS_EMPTY
                    modifiers = DescriptorRendererModifier.ALL - DescriptorRendererModifier.ACTUAL
                    excludedTypeAnnotationClasses = setOf(StandardNames.FqNames.unsafeVariance)
                    annotationFilter = ::isSignificantAnnotation
                }
            )
        }

        private fun isSignificantAnnotation(annotation: AnnotationDescriptor): Boolean {
            // Do not render annotations with error classifiers. Sometimes builtins reference annotations with missing classes, e.g. @OptIn.
            if (annotation.type.isError) return false

            // Do not render SOURCE-retention annotations because they are not serialized to metadata.
            if (annotation.isSourceAnnotation) return false

            // Do not render @ExperimentalStdlibApi, because this annotation is serialized via a hack in BuiltInsSerializerExtension, but
            // when analyzing sources, it's unresolved (as should be) and thus has error type, so it's filtered out.
            if (annotation.fqName?.asString() == "kotlin.ExperimentalStdlibApi") return false

            return true
        }
    }
}
