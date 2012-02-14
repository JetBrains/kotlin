/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.lang.cfg.pseudocode;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.cfg.LoopInfo;
import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetExpression;

/**
 * @author abreslav
 */
public interface JetPseudocodeTrace {

    JetPseudocodeTrace EMPTY = new JetPseudocodeTrace() {
        @Override
        public void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode) {
        }

        @Override
        public void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction) {

        }

        @Override
        public void close() {
        }

        @Override
        public void recordLoopInfo(JetExpression expression, LoopInfo blockInfo) {

        }
    };

    void recordControlFlowData(@NotNull JetElement element, @NotNull Pseudocode pseudocode);
    void recordRepresentativeInstruction(@NotNull JetElement element, @NotNull Instruction instruction);
    void close();

    void recordLoopInfo(JetExpression expression, LoopInfo blockInfo);
}
