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

package org.jetbrains.jet.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

public interface IStackValue {

    void moveToTopOfStack(@NotNull Type type, @NotNull InstructionAdapter v, int depth);

    void put(@NotNull Type type, @NotNull InstructionAdapter v);

    void put(@NotNull Type type, @NotNull InstructionAdapter v, boolean skipReceiver);

    void storeSelector(@NotNull Type topOfStackType, @NotNull InstructionAdapter v);

    void store(@NotNull StackValue value, @NotNull InstructionAdapter v);

    void store(@NotNull StackValue value, @NotNull InstructionAdapter v, boolean skipReceiver);

    void condJump(@NotNull Label label, boolean jumpIfFalse, @NotNull InstructionAdapter v);

    void coerceTo(@NotNull Type toType, @NotNull InstructionAdapter v);

    void coerceFrom(@NotNull Type topOfStackType, @NotNull InstructionAdapter v);

    void putAsBoolean(InstructionAdapter v);

    void dup(@NotNull InstructionAdapter v, boolean withReceiver);
}
