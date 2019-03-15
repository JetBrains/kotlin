/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.codegen;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.backend.common.CodegenUtil;
import org.jetbrains.kotlin.codegen.state.GenerationState;
import org.jetbrains.kotlin.codegen.state.KotlinTypeMapper;
import org.jetbrains.kotlin.descriptors.ClassDescriptor;
import org.jetbrains.kotlin.descriptors.PropertyDescriptor;
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry;
import org.jetbrains.kotlin.psi.KtExpression;
import org.jetbrains.kotlin.psi.KtSuperTypeListEntry;
import org.jetbrains.kotlin.resolve.BindingContext;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.jetbrains.kotlin.load.java.JvmAbi.DELEGATE_SUPER_FIELD_PREFIX;

public class DelegationFieldsInfo {
    private final Type classAsmType;
    private final ClassDescriptor classDescriptor;
    private final GenerationState state;
    private final KotlinTypeMapper typeMapper;
    private final BindingContext bindingContext;

    public DelegationFieldsInfo(
            @NotNull Type type,
            @NotNull ClassDescriptor descriptor,
            @NotNull GenerationState state,
            @NotNull BindingContext context
    ) {
        this.classAsmType = type;
        this.classDescriptor = descriptor;
        this.state = state;
        this.typeMapper = state.getTypeMapper();
        this.bindingContext = context;
    }

    public class Field {
        public final Type type;
        public final String name;
        public final boolean generateField;

        private Field(Type type, String name, boolean generateField) {
            this.type = type;
            this.name = name;
            this.generateField = generateField;
        }

        @NotNull
        public StackValue getStackValue() {
            return StackValue.field(type, classAsmType, name, false, StackValue.none());
        }
    }
    private final Map<KtDelegatedSuperTypeEntry, Field> fields = new HashMap<>();

    @Nullable
    public DelegationFieldsInfo.Field getInfo(KtDelegatedSuperTypeEntry specifier) {
        DelegationFieldsInfo.Field field = fields.get(specifier);
        assert field != null || state.getClassBuilderMode() == ClassBuilderMode.LIGHT_CLASSES : "No field for " + specifier.getText();
        return field;
    }

    private void addField(KtDelegatedSuperTypeEntry specifier, PropertyDescriptor propertyDescriptor) {
        fields.put(specifier,
                   new DelegationFieldsInfo.Field(typeMapper.mapType(propertyDescriptor), propertyDescriptor.getName().asString(), false));
    }

    private void addField(KtDelegatedSuperTypeEntry specifier, Type type, String name) {
        fields.put(specifier, new DelegationFieldsInfo.Field(type, name, true));
    }

    @NotNull
    public DelegationFieldsInfo getDelegationFieldsInfo(@NotNull List<KtSuperTypeListEntry> delegationSpecifiers) {
        DelegationFieldsInfo result = new DelegationFieldsInfo(classAsmType, classDescriptor, state, bindingContext);
        int n = 0;
        for (KtSuperTypeListEntry specifier : delegationSpecifiers) {
            if (specifier instanceof KtDelegatedSuperTypeEntry) {
                KtExpression expression = ((KtDelegatedSuperTypeEntry) specifier).getDelegateExpression();
                if (expression == null) continue;

                PropertyDescriptor propertyDescriptor = CodegenUtil.getDelegatePropertyIfAny(expression, classDescriptor, bindingContext);


                if (CodegenUtil.isFinalPropertyWithBackingField(propertyDescriptor, bindingContext)) {
                    result.addField((KtDelegatedSuperTypeEntry) specifier, propertyDescriptor);
                }
                else {
                    KotlinType expressionType = bindingContext.getType(expression);
                    ClassDescriptor superClass = JvmCodegenUtil.getSuperClass(specifier, state, bindingContext);
                    Type asmType =
                            expressionType != null ? typeMapper.mapType(expressionType) :
                            superClass != null ? typeMapper.mapType(superClass) : null;

                    if (asmType == null) continue;

                    result.addField((KtDelegatedSuperTypeEntry) specifier, asmType, DELEGATE_SUPER_FIELD_PREFIX + n);
                }
                n++;
            }
        }
        return result;
    }
}
