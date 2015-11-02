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

package org.jetbrains.kotlin.idea.debugger.breakpoints.dialog

import com.intellij.psi.PsiClass
import org.jetbrains.kotlin.asJava.KtLightClass
import org.jetbrains.kotlin.asJava.KtLightClassForFacade
import org.jetbrains.kotlin.idea.caches.resolve.resolveToDescriptor
import org.jetbrains.kotlin.idea.core.util.DescriptorMemberChooserObject
import org.jetbrains.kotlin.psi.KtProperty

fun PsiClass.collectProperties(): Array<DescriptorMemberChooserObject> {
    if (this is KtLightClassForFacade) {
        val result = arrayListOf<DescriptorMemberChooserObject>()
        this.files.forEach {
            it.getDeclarations().filterIsInstance<KtProperty>().forEach {
                result.add(DescriptorMemberChooserObject(it, it.resolveToDescriptor()))
            }
        }
        return result.toTypedArray()
    }
    if (this is KtLightClass) {
        val origin = this.getOrigin()
        if (origin != null) {
            return origin.getDeclarations().filterIsInstance<KtProperty>().map {
                DescriptorMemberChooserObject(it, it.resolveToDescriptor())
            }.toTypedArray()
        }
    }
    return emptyArray()
}

