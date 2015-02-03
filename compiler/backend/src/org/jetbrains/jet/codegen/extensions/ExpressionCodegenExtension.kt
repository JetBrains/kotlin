/*
 * Copyright 2010-2014 JetBrains s.r.o.
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

package org.jetbrains.jet.codegen.extensions

import org.jetbrains.jet.extensions.ProjectExtensionDescriptor
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.codegen.StackValue
import org.jetbrains.jet.codegen.state.JetTypeMapper
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter
import org.jetbrains.jet.codegen.ClassBodyCodegen
import org.jetbrains.jet.codegen.ClassBuilder
import org.jetbrains.jet.lang.psi.JetClassOrObject
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor

public trait ExpressionCodegenExtension {
    class object : ProjectExtensionDescriptor<ExpressionCodegenExtension>("org.jetbrains.kotlin.expressionCodegenExtension", javaClass<ExpressionCodegenExtension>())

    public class Context(
            public val typeMapper: JetTypeMapper,
            public val v: InstructionAdapter
    )

    // return null if not applicable
    public fun apply(receiver: StackValue, resolvedCall: ResolvedCall<*>, c: ExpressionCodegenExtension.Context): StackValue?

    public fun generateClassSyntheticParts(codegen: ClassBuilder, clazz: JetClassOrObject, descriptor: DeclarationDescriptor) {

    }
}