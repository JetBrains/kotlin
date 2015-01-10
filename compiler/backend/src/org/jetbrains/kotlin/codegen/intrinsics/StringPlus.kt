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

package org.jetbrains.kotlin.codegen.intrinsics

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.codegen.ExpressionCodegen
import org.jetbrains.kotlin.codegen.StackValue
import org.jetbrains.kotlin.psi.JetExpression
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.jet.lang.resolve.java.AsmTypes.JAVA_STRING_TYPE
import org.jetbrains.jet.lang.resolve.java.AsmTypes.OBJECT_TYPE

public class StringPlus : LazyIntrinsicMethod() {
    override fun generateImpl(
            codegen: ExpressionCodegen,
            returnType: Type,
            element: PsiElement?,
            arguments: List<JetExpression>,
            receiver: StackValue
    ): StackValue {
        return StackValue.operation(JAVA_STRING_TYPE) {
            if (receiver == StackValue.none()) {
                codegen.gen(arguments.get(0)).put(JAVA_STRING_TYPE, it)
                codegen.gen(arguments.get(1)).put(OBJECT_TYPE, it)
            }
            else {
                receiver.put(JAVA_STRING_TYPE, it)
                codegen.gen(arguments.get(0)).put(OBJECT_TYPE, it)
            }
            it.invokestatic(IntrinsicMethods.INTRINSICS_CLASS_NAME, "stringPlus", "(Ljava/lang/String;Ljava/lang/Object;)Ljava/lang/String;", false)
        }
    }
}
