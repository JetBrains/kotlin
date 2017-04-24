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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiManager
import org.jetbrains.kotlin.asJava.builder.LightClassData

abstract class KtLazyLightClass(manager: PsiManager) : KtLightClassBase(manager) {
    abstract val lightClassData: LightClassData

    override val clsDelegate: PsiClass by lazyPub { lightClassData.clsDelegate }

    override fun getOwnFields() = lightClassData.getOwnFields(this)
    override fun getOwnMethods() = lightClassData.getOwnMethods(this)
}