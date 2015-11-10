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

package org.jetbrains.kotlin.idea.presentation

import com.google.common.base.Function
import com.google.common.collect.Collections2
import com.intellij.navigation.ItemPresentation
import com.intellij.navigation.ItemPresentationProvider
import org.apache.commons.lang.StringUtils
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtFunctionLiteral

class KtFunctionPresenter : ItemPresentationProvider<KtFunction> {
    override fun getPresentation(function: KtFunction): ItemPresentation? {
        if (function is KtFunctionLiteral) return null

        return object : KotlinDefaultNamedDeclarationPresentation(function) {
            override fun getPresentableText(): String {
                return buildString {
                    function.name?.let { append(it) }

                    val paramsStrings = Collections2.transform(function.valueParameters, Function<org.jetbrains.kotlin.psi.KtParameter, kotlin.String> { parameter ->
                        if (parameter != null) {
                            val reference = parameter.typeReference
                            if (reference != null) {
                                val text = reference.text
                                if (text != null) {
                                    return@Function text
                                }
                            }
                        }

                        "?"
                    })

                    append("(").append(StringUtils.join(paramsStrings, ",")).append(")")
                }
            }

            override fun getLocationString(): String {
                if (function is KtConstructor<*>) {
                    val name = function.getContainingClassOrObject().fqName ?: return ""
                    return "(in $name)"
                }

                val name = function.fqName ?: return ""
                val receiverTypeRef = function.receiverTypeReference
                val extensionLocation = if (receiverTypeRef != null) "for " + receiverTypeRef.text + " " else ""
                return "(%sin %s)".format(extensionLocation, name.parent())
            }
        }
    }
}
