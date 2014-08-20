/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

import com.intellij.util.ArrayUtil;
import kotlin.Function0;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.context.FieldOwnerContext;
import org.jetbrains.jet.codegen.state.GenerationState;
import org.jetbrains.jet.lang.psi.JetDeclaration;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetNamedFunction;
import org.jetbrains.jet.lang.psi.JetProperty;
import org.jetbrains.org.objectweb.asm.Type;

import static org.jetbrains.jet.codegen.AsmUtil.writeKotlinSyntheticClassAnnotation;
import static org.jetbrains.jet.lang.resolve.java.JvmAnnotationNames.KotlinSyntheticClass;
import static org.jetbrains.org.objectweb.asm.Opcodes.*;

public class PackagePartCodegen extends MemberCodegen<JetFile> {
    private final Type packagePartType;

    public PackagePartCodegen(
            @NotNull ClassBuilder v,
            @NotNull JetFile file,
            @NotNull Type packagePartType,
            @NotNull FieldOwnerContext context,
            @NotNull GenerationState state
    ) {
        super(state, null, context, file, v);
        this.packagePartType = packagePartType;
    }

    @Override
    protected void generateDeclaration() {
        v.defineClass(element, V1_6,
                      ACC_PUBLIC | ACC_FINAL,
                      packagePartType.getInternalName(),
                      null,
                      "java/lang/Object",
                      ArrayUtil.EMPTY_STRING_ARRAY
        );
        v.visitSource(element.getName(), null);

        generatePropertyMetadataArrayFieldIfNeeded(packagePartType);
    }

    @Override
    protected void generateBody() {
        for (JetDeclaration declaration : element.getDeclarations()) {
            if (declaration instanceof JetNamedFunction || declaration instanceof JetProperty) {
                genFunctionOrProperty(declaration);
            }
        }

        if (state.getClassBuilderMode() == ClassBuilderMode.FULL) {
            generateInitializers(new Function0<ExpressionCodegen>() {
                @Override
                public ExpressionCodegen invoke() {
                    return createOrGetClInitCodegen();
                }
            });
        }
    }

    @Override
    protected void generateKotlinAnnotation() {
        writeKotlinSyntheticClassAnnotation(v, KotlinSyntheticClass.Kind.PACKAGE_PART);
    }
}
