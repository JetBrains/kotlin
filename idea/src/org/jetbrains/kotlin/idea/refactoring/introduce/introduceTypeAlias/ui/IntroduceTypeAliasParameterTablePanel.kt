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

package org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.ui

import org.jetbrains.kotlin.idea.refactoring.introduce.introduceTypeAlias.TypeParameter
import org.jetbrains.kotlin.idea.refactoring.introduce.ui.AbstractParameterTablePanel
import java.util.*

open class IntroduceTypeAliasParameterTablePanel : AbstractParameterTablePanel<TypeParameter, IntroduceTypeAliasParameterTablePanel.TypeParameterInfo>() {
    class TypeParameterInfo(
            originalParameter: TypeParameter
    ) : AbstractParameterTablePanel.AbstractParameterInfo<TypeParameter>(originalParameter) {
        init {
            name = originalParameter.name
        }

        override fun toParameter() = originalParameter.copy(name)
    }

    fun init(parameters: List<TypeParameter>) {
        parameterInfos = parameters.mapTo(ArrayList(), ::TypeParameterInfo)
        super.init()
    }

    override fun isCheckMarkColumnEditable() = false

    val selectedTypeParameterInfos: List<TypeParameterInfo>
        get() = parameterInfos.filter { it.isEnabled }

    val selectedTypeParameters: List<TypeParameter>
        get() = selectedTypeParameterInfos.map { it.toParameter() }
}