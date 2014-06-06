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

package org.jetbrains.jet.lang.cfg.pseudocode.instructions.eval

import org.jetbrains.jet.lang.psi.JetElement
import org.jetbrains.jet.lang.cfg.pseudocode.PseudoValue
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.LexicalScope
import org.jetbrains.jet.lang.cfg.pseudocode.instructions.InstructionWithNext
import org.jetbrains.jet.lang.resolve.calls.model.ResolvedCall
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.jet.lang.resolve.calls.tasks.ExplicitReceiverKind
import java.util.Collections

public trait AccessTarget {
    public data class Declaration(val descriptor: VariableDescriptor): AccessTarget
    public data class Call(val resolvedCall: ResolvedCall<*>): AccessTarget
    public object BlackBox: AccessTarget
}

public abstract class AccessValueInstruction protected (
        element: JetElement,
        lexicalScope: LexicalScope,
        public val target: AccessTarget,
        public override val receiverValues: Map<PseudoValue, ReceiverValue>
) : InstructionWithNext(element, lexicalScope), InstructionWithReceivers