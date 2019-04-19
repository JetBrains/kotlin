/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionTransformer
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.checkers.BigFunctionTypeAvailabilityChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.resolve.jvm.checkers.*
import org.jetbrains.kotlin.resolve.jvm.multiplatform.JavaActualAnnotationArgumentExtractor
import org.jetbrains.kotlin.synthetic.JavaSyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings
import org.jetbrains.kotlin.types.expressions.FunctionWithBigAritySupport

object JvmPlatformConfigurator : PlatformConfiguratorBase(
    DynamicTypesSettings(),
    additionalDeclarationCheckers = listOf(
        JvmNameAnnotationChecker(),
        VolatileAnnotationChecker(),
        SynchronizedAnnotationChecker(),
        LocalFunInlineChecker(),
        ExternalFunChecker(),
        OverloadsAnnotationChecker(),
        JvmFieldApplicabilityChecker(),
        TypeParameterBoundIsNotArrayChecker(),
        JvmSyntheticApplicabilityChecker(),
        StrictfpApplicabilityChecker(),
        ExpectedActualDeclarationChecker(listOf(JavaActualAnnotationArgumentExtractor())),
        JvmAnnotationsTargetNonExistentAccessorChecker(),
        BadInheritedJavaSignaturesChecker,
        JvmMultifileClassStateChecker,
        SynchronizedOnInlineMethodChecker
    ),

    additionalCallCheckers = listOf(
        MissingBuiltInDeclarationChecker,
        JavaAnnotationCallChecker(),
        SuspensionPointInsideMutexLockChecker(),
        JavaClassOnCompanionChecker(),
        ProtectedInSuperClassCompanionCallChecker(),
        UnsupportedSyntheticCallableReferenceChecker(),
        SuperCallWithDefaultArgumentsChecker(),
        ProtectedSyntheticExtensionCallChecker,
        RuntimeAssertionsOnExtensionReceiverCallChecker,
        RuntimeAssertionsOnGenericTypeReturningFunctionsCallChecker,
        ApiVersionIsAtLeastArgumentsChecker,
        InconsistentOperatorFromJavaCallChecker
    ),

    additionalTypeCheckers = listOf(
        JavaNullabilityChecker(),
        RuntimeAssertionsTypeChecker,
        JavaGenericVarianceViolationTypeChecker,
        JavaTypeAccessibilityChecker(),
        JvmArrayVariableInLoopAssignmentChecker
    ),

    additionalClassifierUsageCheckers = listOf(
        BigFunctionTypeAvailabilityChecker,
        MissingBuiltInDeclarationChecker.ClassifierUsage
    ),

    additionalAnnotationCheckers = listOf(
        RepeatableAnnotationChecker,
        FileClassAnnotationsChecker,
        ExplicitMetadataChecker
    ),

    identifierChecker = JvmSimpleNameBacktickChecker,

    overloadFilter = JvmOverloadFilter,

    platformToKotlinClassMap = JavaToKotlinClassMap,

    delegationFilter = JvmDelegationFilter,

    overridesBackwardCompatibilityHelper = JvmOverridesBackwardCompatibilityHelper,

    declarationReturnTypeSanitizer = JvmDeclarationReturnTypeSanitizer
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useImpl<JvmStaticChecker>()
        container.useImpl<JvmReflectionAPICallChecker>()
        container.useImpl<JavaSyntheticScopes>()
        container.useImpl<SamConversionResolverImpl>()
        container.useImpl<InterfaceDefaultMethodCallChecker>()
        container.useImpl<JvmDefaultChecker>()
        container.useImpl<InlinePlatformCompatibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker.ClassifierUsage>()
        container.useInstance(JvmTypeSpecificityComparator)
        container.useImpl<JvmDefaultSuperCallChecker>()
        container.useImpl<JvmSamConversionTransformer>()
        container.useInstance(FunctionWithBigAritySupport.LANGUAGE_VERSION_DEPENDENT)
    }
}
