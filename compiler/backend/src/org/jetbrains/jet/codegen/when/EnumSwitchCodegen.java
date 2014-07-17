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
