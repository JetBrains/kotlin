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

package org.jetbrains.kotlin.idea.presentation

import com.intellij.psi.presentation.java.ClassPresentationUtil
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.name.FqName

class KtLightClassListCellRenderer : KtModuleSpecificListCellRenderer<KtLightClass>() {
    override fun getElementText(element: KtLightClass) = ClassPresentationUtil.getNameForClass(element, false)

    // TODO: correct text for local, anonymous, enum entries ... etc
    override fun getContainerText(element: KtLightClass, name: String) = element.qualifiedName?.let { qName ->
        "(" + FqName(qName).parent().asString() + ")"
    } ?: ""
}
