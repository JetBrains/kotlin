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

package org.jetbrains.kotlin.idea.inspections

import org.jetbrains.kotlin.idea.KotlinBundle
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter

/**
 * @return string description of declaration, like `Function "describe"`
 */
fun KtNamedDeclaration.describe(): String? = when (this) {
    is KtClass -> "${if (isInterface()) KotlinBundle.message("interface") else KotlinBundle.message("class")} \"$name\""
    is KtObjectDeclaration -> KotlinBundle.message("object.0", name.toString())
    is KtNamedFunction -> KotlinBundle.message("function.01", name.toString())
    is KtSecondaryConstructor -> KotlinBundle.message("constructor")
    is KtProperty -> KotlinBundle.message("property.0", name.toString())
    is KtParameter -> if (this.isPropertyParameter())
        KotlinBundle.message("property.0", name.toString())
    else
        KotlinBundle.message("parameter.0", name.toString())
    is KtTypeParameter -> KotlinBundle.message("type.parameter.0", name.toString())
    is KtTypeAlias -> KotlinBundle.message("type.alias.0", name.toString())
    else -> null
}