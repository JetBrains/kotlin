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

package org.jetbrains.kotlin.psi.findDocComment

import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationModifierList
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.psiUtil.allChildren

fun findDocComment(declaration: KtDeclaration): KDoc? {
    val containingFile = declaration.containingFile
    if (containingFile is KtFile && containingFile.isCompiled) {
        //can't use containingKtFile due to non-physical code fragments, e.g. ssr
        return null
    }
    return declaration.allChildren
        .flatMap {
            if (it is KtDeclarationModifierList) {
                return@flatMap it.children.asSequence()
            }
            sequenceOf(it)
        }
        .dropWhile { it !is KDoc }
        .firstOrNull() as? KDoc
}
