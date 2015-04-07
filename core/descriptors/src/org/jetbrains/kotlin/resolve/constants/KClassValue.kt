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

package org.jetbrains.kotlin.resolve.constants

import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.annotations.AnnotationArgumentVisitor
import org.jetbrains.kotlin.types.JetType


public class KClassValue(type: JetType) : CompileTimeConstant<JetType>(type, true, false, false) {
    override fun getType(kotlinBuiltIns: KotlinBuiltIns) = value
    override fun getValue(): JetType = value.getArguments().single().getType()

    override fun <R, D> accept(visitor: AnnotationArgumentVisitor<R, D>, data: D) = visitor.visitKClassValue(this, data)
}
