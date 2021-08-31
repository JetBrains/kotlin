/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.builtins.jvm.JavaToKotlinClassMapper
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.container.*
import org.jetbrains.kotlin.load.java.sam.JvmSamConversionOracle
import org.jetbrains.kotlin.resolve.PlatformConfiguratorBase
import org.jetbrains.kotlin.resolve.checkers.BigFunctionTypeAvailabilityChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.resolve.jvm.checkers.*
import org.jetbrains.kotlin.resolve.jvm.multiplatform.JavaActualAnnotationArgumentExtractor
import org.jetbrains.kotlin.resolve.sam.SamConversionResolver
import org.jetbrains.kotlin.resolve.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.synthetic.JavaSyntheticScopes
import org.jetbrains.kotlin.types.expressions.FunctionWithBigAritySupport
import org.jetbrains.kotlin.types.expressions.GenericArrayClassLiteralSupport

object JvmPlatformConfigurator : PlatformConfiguratorBase(
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
        JvmInlineApplicabilityChecker(),
        StrictfpApplicabilityChecker(),
        JvmAnnotationsTargetNonExistentAccessorChecker(),
        SuspendInFunInterfaceChecker(),
        BadInheritedJavaSignaturesChecker,
        JvmMultifileClassStateChecker,
        DefaultCheckerInTailrec,
        FunctionDelegateMemberNameClashChecker,
        ClassInheritsJavaSealedClassChecker
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
        ApiVersionIsAtLeastArgumentsChecker,
        InconsistentOperatorFromJavaCallChecker,
        PolymorphicSignatureCallChecker,
    ),

    additionalTypeCheckers = listOf(
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
        FileClassAnnotationsChecker,
        ExplicitMetadataChecker,
        SynchronizedAnnotationOnLambdaChecker,
    ),

    additionalClashResolvers = listOf(
        PlatformExtensionsClashResolver.FallbackToDefault(SamConversionResolver.Empty, SamConversionResolver::class.java)
    ),

    identifierChecker = JvmSimpleNameBacktickChecker,

    overloadFilter = JvmOverloadFilter,

    platformToKotlinClassMapper = JavaToKotlinClassMapper,

    delegationFilter = JvmDelegationFilter,

    overridesBackwardCompatibilityHelper = JvmOverridesBackwardCompatibilityHelper,

    declarationReturnTypeSanitizer = JvmDeclarationReturnTypeSanitizer
) {
    override fun configureModuleComponents(container: StorageComponentContainer, languageVersionSettings: LanguageVersionSettings) {
        container.useImplIf<WarningAwareUpperBoundChecker>(
            !languageVersionSettings.supportsFeature(LanguageFeature.TypeEnhancementImprovementsInStrictMode)
        )

        container.useImpl<JavaNullabilityChecker>()
        container.useImpl<JvmStaticChecker>()
        container.useImpl<JvmReflectionAPICallChecker>()
        container.useImpl<JavaSyntheticScopes>()
        container.useImpl<SamConversionResolverImpl>()
        container.useImpl<InterfaceDefaultMethodCallChecker>()
        container.useImpl<JvmDefaultChecker>()
        container.useImpl<InlinePlatformCompatibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker.ClassifierUsage>()
        container.useImpl<JvmTypeSpecificityComparatorDelegate>()
        container.useImpl<JvmPlatformOverloadsSpecificityComparator>()
        container.useImpl<JvmDefaultSuperCallChecker>()
        container.useImpl<JvmSamConversionOracle>()
        container.useImpl<JvmAdditionalClassPartsProvider>()
        container.useImpl<JvmRecordApplicabilityChecker>()
        container.useImpl<JvmPlatformAnnotationFeaturesSupport>()

        container.useInstance(FunctionWithBigAritySupport.LanguageVersionDependent)
        container.useInstance(GenericArrayClassLiteralSupport.Enabled)
        container.useInstance(JavaActualAnnotationArgumentExtractor())
    }

    override fun configureModuleDependentCheckers(container: StorageComponentContainer) {
        super.configureModuleDependentCheckers(container)
        container.useImpl<ExpectedActualDeclarationChecker>()
        container.useImpl<RepeatableAnnotationChecker>()
    }
}
