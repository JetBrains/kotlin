/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve.jvm.platform

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useImpl
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.load.java.sam.SamConversionResolverImpl
import org.jetbrains.kotlin.platform.JavaToKotlinClassMap
import org.jetbrains.kotlin.resolve.PlatformConfigurator
import org.jetbrains.kotlin.resolve.calls.checkers.ReifiedTypeParameterSubstitutionChecker
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.jvm.*
import org.jetbrains.kotlin.resolve.jvm.checkers.*
import org.jetbrains.kotlin.synthetic.JavaSyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings

object JvmPlatformConfigurator : PlatformConfigurator(
        DynamicTypesSettings(),
        additionalDeclarationCheckers = listOf(
                PlatformStaticAnnotationChecker(),
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
                ExpectedActualDeclarationChecker
        ),

        additionalCallCheckers = listOf(
                JavaAnnotationCallChecker(),
                JavaClassOnCompanionChecker(),
                ProtectedInSuperClassCompanionCallChecker(),
                UnsupportedSyntheticCallableReferenceChecker(),
                SuperCallWithDefaultArgumentsChecker(),
                ProtectedSyntheticExtensionCallChecker,
                ReifiedTypeParameterSubstitutionChecker(),
                RuntimeAssertionsOnExtensionReceiverCallChecker,
                ApiVersionIsAtLeastArgumentsChecker
        ),

        additionalTypeCheckers = listOf(
                JavaNullabilityChecker(),
                RuntimeAssertionsTypeChecker,
                JavaGenericVarianceViolationTypeChecker,
                JavaTypeAccessibilityChecker()
        ),

        additionalClassifierUsageCheckers = listOf(
        ),

        additionalAnnotationCheckers = listOf(
                RepeatableAnnotationChecker,
                FileClassAnnotationsChecker
        ),

        identifierChecker = JvmSimpleNameBacktickChecker,

        overloadFilter = JvmOverloadFilter,

        platformToKotlinClassMap = JavaToKotlinClassMap,

        delegationFilter = JvmDelegationFilter,

        overridesBackwardCompatibilityHelper = JvmOverridesBackwardCompatibilityHelper,

        declarationReturnTypeSanitizer = JvmDeclarationReturnTypeSanitizer
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useImpl<JvmReflectionAPICallChecker>()
        container.useImpl<JavaSyntheticScopes>()
        container.useImpl<SamConversionResolverImpl>()
        container.useImpl<InterfaceDefaultMethodCallChecker>()
        container.useImpl<InlinePlatformCompatibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker>()
        container.useImpl<JvmModuleAccessibilityChecker.ClassifierUsage>()
        container.useInstance(JvmTypeSpecificityComparator)
    }
}
