/*
 * Copyright 2000-2014 JetBrains s.r.o.
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
package org.jetbrains.kotlin.idea.debugger.evaluate;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.org.objectweb.asm.ClassReader;
import org.jetbrains.org.objectweb.asm.ClassVisitor;
import org.jetbrains.org.objectweb.asm.ClassWriter;
import org.jetbrains.org.objectweb.asm.Opcodes;

public class CompilingEvaluatorUtils {
    // Copied from com.intellij.debugger.ui.impl.watch.CompilingEvaluator.changeSuperToMagicAccessor
    public static byte[] changeSuperToMagicAccessor(byte[] bytes) {
        ClassWriter classWriter = new ClassWriter(0);
        ClassVisitor classVisitor = new ClassVisitor(Opcodes.API_VERSION, classWriter) {
            @Override
            public void visit(int version, int access, @NotNull String name, String signature, String superName, String[] interfaces) {
                if ("java/lang/Object".equals(superName)) {
                    superName = "sun/reflect/MagicAccessorImpl";
                }
                super.visit(version, access, name, signature, superName, interfaces);
            }
        };
        new ClassReader(bytes).accept(classVisitor, 0);
        return classWriter.toByteArray();
    }
}
