/*
 * Copyright 2000-2012 JetBrains s.r.o.
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

import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.InstructionAdapter;

/**
 * @author Stepan Koltsov
 */
public class StubCodegen {
    public static void generateStubThrow(MethodVisitor mv) {
        new InstructionAdapter(mv).anew(Type.getObjectType("java/lang/RuntimeException"));
        new InstructionAdapter(mv).dup();
        new InstructionAdapter(mv).aconst("Stubs are for compiler only, do not add them to runtime classpath");
        new InstructionAdapter(mv).invokespecial("java/lang/RuntimeException", "<init>", "(Ljava/lang/String;)V");
        new InstructionAdapter(mv).athrow();
    }

    public static void generateStubCode(MethodVisitor mv) {
        mv.visitCode();
        generateStubThrow(mv);
        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }
}
