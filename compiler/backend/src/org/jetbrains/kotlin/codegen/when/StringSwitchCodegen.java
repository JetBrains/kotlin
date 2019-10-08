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
import org.jetbrains.kotlin.psi.KtWhenEntry;
import org.jetbrains.kotlin.psi.KtWhenExpression;
import org.jetbrains.kotlin.resolve.constants.ConstantValue;
import org.jetbrains.kotlin.resolve.constants.StringValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class StringSwitchCodegen extends SwitchCodegen {
    private static final String HASH_CODE_METHOD_DESC = Type.getMethodDescriptor(Type.INT_TYPE);
    private static final String EQUALS_METHOD_DESC = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class));

    private final Map<Integer, List<Entry>> hashCodesToEntries = new HashMap<>();
    private int tempVarIndex;

    public StringSwitchCodegen(
            @NotNull KtWhenExpression expression,
            boolean isStatement,
            boolean isExhaustive,
            @NotNull ExpressionCodegen codegen
    ) {
        super(expression, isStatement, isExhaustive, codegen, null);
    }

    @Override
    protected void processConstant(@NotNull ConstantValue<?> constant, @NotNull Label entryLabel, @NotNull KtWhenEntry entry) {
        assert constant instanceof StringValue : "guaranteed by usage contract";
        int hashCode = constant.hashCode();

        if (!transitionsTable.containsKey(hashCode)) {
            transitionsTable.put(hashCode, new Label());
            hashCodesToEntries.put(hashCode, new ArrayList<>());
        }

        hashCodesToEntries.get(hashCode).add(new Entry(((StringValue) constant).getValue(), entryLabel, entry));
    }

    @Override
    public void generate() {
        super.generate();
        codegen.myFrameMap.leaveTemp(subjectType);
    }

    @Override
    protected void generateSubjectValueToIndex() {
        generateNullCheckIfNeeded();

        tempVarIndex = codegen.myFrameMap.enterTemp(subjectType);
        v.store(tempVarIndex, subjectType);
        v.load(tempVarIndex, subjectType);

        v.invokevirtual(
                subjectType.getInternalName(),
                "hashCode", HASH_CODE_METHOD_DESC, false
        );
    }

    @Override
    protected void generateEntries() {
        for (int hashCode : hashCodesToEntries.keySet()) {
            v.visitLabel(transitionsTable.get(hashCode));

            List<Entry> items = hashCodesToEntries.get(hashCode);
            Label nextLabel = null;

            for (int i = 0; i < items.size(); i++) {
                if (nextLabel != null) {
                    v.visitLabel(nextLabel);
                }

                Entry entry = items.get(i);

                codegen.markLineNumber(entry.entry, false);
                v.load(tempVarIndex, subjectType);
                v.aconst(entry.value);
                v.invokevirtual(
                        subjectType.getInternalName(),
                        "equals",
                        EQUALS_METHOD_DESC,
                        false
                );

                if (i + 1 < items.size()) {
                    nextLabel = new Label();
                }
                else {
                    nextLabel = defaultLabel;
                }

                v.ifeq(nextLabel);
                v.goTo(entry.label);
            }
        }

        super.generateEntries();
    }

    private static class Entry {
        private final String value;
        private final Label label;
        private final KtWhenEntry entry;

        private Entry(String value, Label label, KtWhenEntry entry) {
            this.value = value;
            this.label = label;
            this.entry = entry;
        }
    }
}
