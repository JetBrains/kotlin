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

package org.jetbrains.kotlin.codegen.when;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.codegen.ExpressionCodegen;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.EnumValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

public class EnumSwitchCodegen extends SwitchCodegen {
    private final WhenByEnumsMapping mapping;

    public EnumSwitchCodegen(
            @NotNull KtWhenExpression expression,
            boolean isStatement,
            boolean isExhaustive,
            @NotNull ExpressionCodegen codegen,
            @NotNull WhenByEnumsMapping mapping
    ) {
        super(expression, isStatement, isExhaustive, codegen, codegen.getState().getTypeMapper().mapType(mapping.getEnumClassDescriptor()));
        this.mapping = mapping;
    }

    @Override
    protected void generateSubjectValueToIndex() {
        codegen.getState().getMappingsClassesForWhenByEnum().generateMappingsClassForExpression(expression);

        generateNullCheckIfNeeded();

        v.getstatic(
                mapping.getMappingsClassInternalName(),
                mapping.getFieldName(),
                MappingClassesForWhenByEnumCodegen.MAPPINGS_FIELD_DESCRIPTOR
        );

        v.swap();

        Type enumType = codegen.getState().getTypeMapper().mapClass(mapping.getEnumClassDescriptor());
        v.invokevirtual(enumType.getInternalName(), "ordinal", Type.getMethodDescriptor(Type.INT_TYPE), false);
        v.aload(Type.INT_TYPE);
    }

    @Override
    protected void processConstant(@NotNull ConstantValue<?> constant, @NotNull Label entryLabel) {
        assert constant instanceof EnumValue : "guaranteed by usage contract";
        putTransitionOnce(mapping.getIndexByEntry((EnumValue) constant), entryLabel);
    }
}
