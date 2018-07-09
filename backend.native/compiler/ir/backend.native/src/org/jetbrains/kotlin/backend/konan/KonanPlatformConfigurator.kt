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

// Adapted from JS compiler, but everyhing has been switched off for now.

package org.jetbrains.kotlin.backend.konan

import org.jetbrains.kotlin.builtins.PlatformToKotlinClassMap
import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.calls.checkers.ReifiedTypeParameterSubstitutionChecker
import org.jetbrains.kotlin.resolve.calls.components.SamConversionTransformer
import org.jetbrains.kotlin.resolve.calls.results.TypeSpecificityComparator
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.lazy.DelegationFilter
import org.jetbrains.kotlin.resolve.scopes.SyntheticScopes
import org.jetbrains.kotlin.types.DynamicTypesSettings


object KonanPlatformConfigurator : PlatformConfigurator(
        DynamicTypesSettings(),
        additionalDeclarationCheckers = listOf(ExpectedActualDeclarationChecker),
        additionalCallCheckers = listOf(
                org.jetbrains.kotlin.resolve.jvm.checkers.SuperCallWithDefaultArgumentsChecker(),
                ReifiedTypeParameterSubstitutionChecker()
        ),
        additionalTypeCheckers = listOf(),
        additionalClassifierUsageCheckers = listOf(),
        additionalAnnotationCheckers = listOf(),
        identifierChecker = IdentifierChecker.Default,
        overloadFilter = OverloadFilter.Default,
        platformToKotlinClassMap = PlatformToKotlinClassMap.EMPTY,
        delegationFilter = DelegationFilter.Default,
        overridesBackwardCompatibilityHelper = OverridesBackwardCompatibilityHelper.Default,
        declarationReturnTypeSanitizer = DeclarationReturnTypeSanitizer.Default
) {
    override fun configureModuleComponents(container: StorageComponentContainer) {
        container.useInstance(SyntheticScopes.Empty)
        container.useInstance(TypeSpecificityComparator.NONE)
        container.useInstance(SamConversionTransformer.Empty)
    }
}
