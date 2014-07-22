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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.EnumValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

public class EnumSwitchCodegen extends SwitchCodegen {
    private final WhenByEnumsMapping mapping;

    public EnumSwitchCodegen(
            @NotNull JetWhenExpression expression,
            boolean isStatement,
            @NotNull ExpressionCodegen codegen,
            @NotNull WhenByEnumsMapping mapping
    ) {
        super(expression, isStatement, codegen);
        this.mapping = mapping;
    }

    @Override
    protected void generateSubject() {
        codegen.getState().getMappingsClassesForWhenByEnum().generateMappingsClassForExpression(expression);

        super.generateSubject();
        generateNullCheckIfNeeded();

        v.getstatic(
                mapping.getMappingsClassInternalName(),
                mapping.getFieldName(),
                MappingClassesForWhenByEnumCodegen.MAPPINGS_FIELD_DESCRIPTOR
        );

        v.swap();

        v.invokevirtual(
                mapping.getEnumClassInternalName(),
                "ordinal",
                Type.getMethodDescriptor(Type.INT_TYPE),
                false
        );
        v.aload(Type.INT_TYPE);
    }

    @Override
    protected void processConstant(
            @NotNull CompileTimeConstant constant,
            @NotNull Label entryLabel
    ) {
        assert constant instanceof EnumValue : "guaranteed by usage contract";
        putTransitionOnce(mapping.getIndexByEntry((EnumValue) constant), entryLabel);
    }
}
