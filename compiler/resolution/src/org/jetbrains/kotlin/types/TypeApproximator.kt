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

package org.jetbrains.kotlin.types

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.resolve.calls.components.ClassicTypeSystemContextForCS

class TypeApproximator(builtIns: KotlinBuiltIns) : AbstractTypeApproximator(ClassicTypeSystemContextForCS(builtIns)) {
    fun approximateDeclarationType(baseType: KotlinType, local: Boolean, languageVersionSettings: LanguageVersionSettings): UnwrappedType {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.NewInference)) return baseType.unwrap()

        val configuration = if (local) TypeApproximatorConfiguration.LocalDeclaration else TypeApproximatorConfiguration.PublicDeclaration
        val preparedType = if (local) baseType.unwrap() else substituteAlternativesInPublicType(baseType)
        return approximateToSuperType(preparedType, configuration) ?: preparedType
    }

    // null means that this input type is the result, i.e. input type not contains not-allowed kind of types
    // type <: resultType
    fun approximateToSuperType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSuperType(type, conf) as UnwrappedType?

    // resultType <: type
    fun approximateToSubType(type: UnwrappedType, conf: TypeApproximatorConfiguration): UnwrappedType? =
        super.approximateToSubType(type, conf) as UnwrappedType?
}

