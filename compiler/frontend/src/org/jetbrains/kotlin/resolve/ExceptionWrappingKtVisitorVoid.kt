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

package org.jetbrains.kotlin.resolve

import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.util.KotlinFrontEndException

class ExceptionWrappingKtVisitorVoid(private val delegate: KtVisitorVoid) : KtVisitorVoid() {
    override fun visitElement(element: PsiElement) {
        element.accept(delegate)
    }

    override fun visitDeclaration(dcl: KtDeclaration) {
        try {
            dcl.accept(delegate)
        }
        catch (e: ProcessCanceledException) {
            throw e
        }
        catch (e: KotlinFrontEndException) {
            throw e
        }
        catch (t: Throwable) {
            throw KotlinFrontEndException("Failed to analyze declaration ${dcl.name}", t, dcl)
        }
    }
}
