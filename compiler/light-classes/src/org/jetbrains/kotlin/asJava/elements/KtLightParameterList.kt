/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.asJava.elements

import com.intellij.psi.*
import com.intellij.psi.impl.light.LightElement
import org.jetbrains.kotlin.asJava.classes.lazyPub
import org.jetbrains.kotlin.idea.KotlinLanguage

class KtLightParameterList(
        private val parent: KtLightMethod,
        private val parametersCount: Int,
        computeParameters: () -> List<PsiParameter>) : LightElement(parent.manager, KotlinLanguage.INSTANCE), PsiParameterList {
    private val _parameters: Array<PsiParameter> by lazyPub { computeParameters().toTypedArray() }

    override fun getParent() = parent

    override fun toString() = "Light parameter list of $parent"

    override fun getParameters() = _parameters

    override fun getParameterIndex(parameter: PsiParameter) = _parameters.indexOf(parameter)

    override fun getParametersCount() = parametersCount

    override fun accept(visitor: PsiElementVisitor) {
        if (visitor is JavaElementVisitor) {
            visitor.visitParameterList(this)
        }
    }
}
