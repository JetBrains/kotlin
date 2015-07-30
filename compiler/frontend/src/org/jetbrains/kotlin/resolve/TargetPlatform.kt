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

package org.jetbrains.kotlin.resolve

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.resolve.calls.checkers.*
import org.jetbrains.kotlin.resolve.validation.DeprecatedSymbolValidator
import org.jetbrains.kotlin.resolve.validation.SymbolUsageValidator
import org.jetbrains.kotlin.types.DynamicTypesSettings

public abstract class TargetPlatform(
        public val platformName: String
) {
    override fun toString(): String {
        return platformName
    }

    public abstract val platformConfigurator: PlatformConfigurator

    public object Default : TargetPlatform("Default") {
        override val platformConfigurator = PlatformConfigurator(DynamicTypesSettings(), listOf(), listOf(), listOf(), listOf())
    }
}

private val DEFAULT_DECLARATION_CHECKERS = listOf(DataClassAnnotationChecker())
private val DEFAULT_CALL_CHECKERS = listOf(CapturingInClosureChecker(), InlineCheckerWrapper(), ReifiedTypeParameterSubstitutionChecker())
private val DEFAULT_TYPE_CHECKERS = emptyList<AdditionalTypeChecker>()
private val DEFAULT_VALIDATORS = listOf(DeprecatedSymbolValidator())


public open class PlatformConfigurator(
        private val dynamicTypesSettings: DynamicTypesSettings,
        additionalDeclarationCheckers: List<DeclarationChecker>,
        additionalCallCheckers: List<CallChecker>,
        additionalTypeCheckers: List<AdditionalTypeChecker>,
        additionalSymbolUsageValidators: List<SymbolUsageValidator>,
        private val additionalAnnotationChecker: AdditionalAnnotationChecker? = null
) {

    private val declarationCheckers: List<DeclarationChecker> = DEFAULT_DECLARATION_CHECKERS + additionalDeclarationCheckers
    private val callCheckers: List<CallChecker> = DEFAULT_CALL_CHECKERS + additionalCallCheckers
    private val typeCheckers: List<AdditionalTypeChecker> = DEFAULT_TYPE_CHECKERS + additionalTypeCheckers
    private val symbolUsageValidator: SymbolUsageValidator = SymbolUsageValidator.Composite(DEFAULT_VALIDATORS + additionalSymbolUsageValidators)

    public open fun configure(container: StorageComponentContainer) {
        with (container) {
            useInstance(dynamicTypesSettings)
            declarationCheckers.forEach { useInstance(it) }
            callCheckers.forEach { useInstance(it) }
            typeCheckers.forEach { useInstance(it) }
            useInstance(symbolUsageValidator)
        }
        AnnotationChecker.additionalChecker = additionalAnnotationChecker
    }
}