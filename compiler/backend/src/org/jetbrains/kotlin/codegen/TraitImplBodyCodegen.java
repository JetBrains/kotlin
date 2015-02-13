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

package org.jetbrains.kotlin.codegen;

import com.intellij.util.ArrayUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.codegen.context.ClassContext;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.psi.JetClassOrObject;
import org.jetbrains.kotlin.resolve.DescriptorUtils;

import static org.jetbrains.kotlin.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation;
import static org.jetbrains.kotlin.load.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class TraitImplBodyCodegen extends ClassBodyCodegen {

    public TraitImplBodyCodegen(
            @NotNull JetClassOrObject aClass,
            @NotNull ClassContext context,
            @NotNull ClassBuilder v,
            @NotNull GenerationState state,
            @Nullable MemberCodegen<?> parentCodegen
    ) {
        super(aClass, context, v, state, parentCodegen);
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(myClass, V1_6,
                      ACC_PUBLIC | ACC_FINAL,
                      typeMapper.mapTraitImpl(descriptor).getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(myClass.getContainingFile().getName(), null);
    }

    @Override
    protected void generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v, DescriptorUtils.isTopLevelOrInnerClass(descriptor)
                                               ? KotlinSyntheticClass.Kind.TRAIT_IMPL
                                               : KotlinSyntheticClass.Kind.LOCAL_TRAIT_IMPL);
    }
}
