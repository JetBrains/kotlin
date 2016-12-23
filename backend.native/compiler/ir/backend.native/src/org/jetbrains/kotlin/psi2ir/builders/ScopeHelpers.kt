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

package org.jetbrains.kotlin.psi2ir.builders

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.psi2ir.generators.Scope

inline fun <reified T> Scope.assertCastOwner() =
        scopeOwner as? T ?:
        throw AssertionError("Unexpected scopeOwner: $scopeOwner")

fun Scope.functionOwner(): FunctionDescriptor =
        assertCastOwner()

fun Scope.classOwner(): ClassDescriptor =
        when (scopeOwner) {
            is ClassDescriptor -> scopeOwner
            is MemberDescriptor -> scopeOwner.containingDeclaration as ClassDescriptor
            else -> throw AssertionError("Unexpected scopeOwner: $scopeOwner")
        }
