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

package org.jetbrains.kotlin.codegen.range.comparison

import org.jetbrains.org.objectweb.asm.Label
import org.jetbrains.org.objectweb.asm.Type
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter

class IntegerComparisonGenerator(override val comparedType: Type) : SignedIntegerComparisonGenerator {
    override fun jumpIfGreaterOrEqual(v: InstructionAdapter, label: Label) {
        v.ificmpge(label)
    }

    override fun jumpIfLessOrEqual(v: InstructionAdapter, label: Label) {
        v.ificmple(label)
    }

    override fun jumpIfGreater(v: InstructionAdapter, label: Label) {
        v.ificmpgt(label)
    }

    override fun jumpIfLess(v: InstructionAdapter, label: Label) {
        v.ificmplt(label)
    }

    override fun jumpIfLessThanZero(v: InstructionAdapter, label: Label) {
        v.iflt(label)
    }
}

val IntComparisonGenerator = IntegerComparisonGenerator(Type.INT_TYPE)
val CharComparisonGenerator = IntegerComparisonGenerator(Type.CHAR_TYPE)