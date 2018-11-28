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

import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.isPropertyParameter

/**
 * @return string description of declaration, like `Function "describe"`
 */
fun KtNamedDeclaration.describe(): String? = when (this) {
    is KtClass -> "${if (isInterface()) "Interface" else "Class"} \"$name\""
    is KtObjectDeclaration -> "Object \"$name\""
    is KtNamedFunction -> "Function \"$name\""
    is KtSecondaryConstructor -> "Constructor"
    is KtProperty -> "Property \"$name\""
    is KtParameter -> if (this.isPropertyParameter()) "Property \"$name\"" else "Parameter \"$name\""
    is KtTypeParameter -> "Type parameter \"$name\""
    is KtTypeAlias -> "Type alias \"$name\""
    else -> null
}