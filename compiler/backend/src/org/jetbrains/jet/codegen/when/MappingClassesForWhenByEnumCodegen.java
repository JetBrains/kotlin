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

package org.jetbrains.jet.codegen.when;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.AsmUtil;
import org.jetbrains.jet.codegen.ClassBuilder;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;
import org.jetbrains.jet.lang.resolve.java.diagnostics.JvmDeclarationOrigin;
import org.jetbrains.org.objectweb.asm.MethodVisitor;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.List;
import java.util.Map;

import static org.jetbrains.jet.lang.resolve.java.AsmTypeConstants.OBJECT_TYPE;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class MappingClassesForWhenByEnumCodegen {
    public static final String MAPPINGS_FIELD_DESCRIPTOR = Type.getDescriptor(int[].class);
    private final GenerationState state;

    public MappingClassesForWhenByEnumCodegen(@NotNull GenerationState state) {
        this.state = state;
    }

    public void generate(
            @NotNull List<WhenByEnumsMapping> mappings,
            @NotNull Type mappingsClass,
            @NotNull JetFile srcFile
    ) {
        ClassBuilder cb = state.getFactory().newVisitor(
                JvmDeclarationOrigin.NO_ORIGIN,
                mappingsClass,
                srcFile
        );
        cb.defineClass(
                srcFile,
                V1_6,
                ACC_FINAL | ACC_SYNTHETIC,
                mappingsClass.getInternalName(),
                null,
                OBJECT_TYPE.getInternalName(),
                ArrayUtil.EMPTY_STRING_ARRAY
        );

        generateFields(cb, mappings);
        generateInitialization(cb, mappings);

        cb.done();
    }

    private static void generateFields(
            @NotNull ClassBuilder cb,
            @NotNull List<WhenByEnumsMapping> mappings
    ) {
        for (WhenByEnumsMapping mapping : mappings) {
            cb.newField(
                    JvmDeclarationOrigin.NO_ORIGIN,
                    ACC_STATIC | ACC_PUBLIC | ACC_FINAL | ACC_SYNTHETIC,
                    mapping.getFieldName(),
                    MAPPINGS_FIELD_DESCRIPTOR,
                    null, null
            );
        }
    }

    private static void generateInitialization(
            @NotNull ClassBuilder cb,
            @NotNull List<WhenByEnumsMapping> mappings
    ) {
        MethodVisitor mv = cb.newMethod(
                JvmDeclarationOrigin.NO_ORIGIN,
                ACC_STATIC | ACC_SYNTHETIC, "<clinit>", "()V", null, ArrayUtil.EMPTY_STRING_ARRAY
        );

        mv.visitCode();

        InstructionAdapter v = new InstructionAdapter(mv);

        for (WhenByEnumsMapping mapping : mappings) {
            generateInitializationForMapping(cb, v, mapping);
        }

        v.areturn(Type.VOID_TYPE);

        mv.visitMaxs(-1, -1);
        mv.visitEnd();
    }

    private static void generateInitializationForMapping(
            @NotNull ClassBuilder cb,
            @NotNull InstructionAdapter v,
            @NotNull WhenByEnumsMapping mapping
    ) {
        v.invokestatic(
                mapping.getEnumClassInternalName(), "values",
                Type.getMethodDescriptor(
                        AsmUtil.getArrayOf(mapping.getEnumClassInternalName())
                ),
                false
        );
        v.arraylength();

        v.newarray(Type.INT_TYPE);
        v.putstatic(cb.getThisName(), mapping.getFieldName(), MAPPINGS_FIELD_DESCRIPTOR);

        String enumClassDesc = Type.getObjectType(mapping.getEnumClassInternalName()).getDescriptor();

        for (Map.Entry<EnumValue, Integer> item : mapping.enumValuesToIntMapping()) {
            EnumValue enumEntry = item.getKey();
            int mappedValue = item.getValue();

            v.getstatic(cb.getThisName(), mapping.getFieldName(), MAPPINGS_FIELD_DESCRIPTOR);
            v.getstatic(
                    mapping.getEnumClassInternalName(),
                    enumEntry.getValue().getName().asString(),
                    enumClassDesc
            );
            v.invokevirtual(
                    mapping.getEnumClassInternalName(), "ordinal",
                    Type.getMethodDescriptor(Type.INT_TYPE),
                    false
            );
            v.iconst(mappedValue);
            v.astore(Type.INT_TYPE);
        }
    }
}
