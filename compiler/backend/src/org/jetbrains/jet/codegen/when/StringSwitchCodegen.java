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

import com.google.common.collect.Maps;
import com.intellij.openapi.util.Pair;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.codegen.ExpressionCodegen;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.StringValue;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StringSwitchCodegen extends SwitchCodegen {
    private static final String HASH_CODE_METHOD_DESC = Type.getMethodDescriptor(Type.INT_TYPE);
    private static final String EQUALS_METHOD_DESC = Type.getMethodDescriptor(Type.BOOLEAN_TYPE, Type.getType(Object.class));

    private final Map<Integer, List<Pair<String, Label>>> hashCodesToStringAndEntryLabel = Maps.newHashMap();
    private int tempVarIndex;

    public StringSwitchCodegen(
            @NotNull JetWhenExpression expression,
            boolean isStatement,
            @NotNull ExpressionCodegen codegen
    ) {
        super(expression, isStatement, codegen);
    }

    @Override
    protected void processConstant(
            @NotNull CompileTimeConstant constant, @NotNull Label entryLabel
    ) {
        assert constant instanceof StringValue : "guaranteed by usage contract";
        int hashCode = constant.hashCode();

        if (!transitionsTable.containsKey(hashCode)) {
            transitionsTable.put(hashCode, new Label());
            hashCodesToStringAndEntryLabel.put(hashCode, new ArrayList<Pair<String, Label>>());
        }

        hashCodesToStringAndEntryLabel.get(hashCode).add(
                new Pair<String, Label>(((StringValue) constant).getValue(), entryLabel)
        );
    }

    @Override
    public void generate() {
        super.generate();
        codegen.myFrameMap.leaveTemp(subjectType);
    }

    @Override
    protected void generateSubject() {
        tempVarIndex = codegen.myFrameMap.enterTemp(subjectType);
        super.generateSubject();
        v.store(tempVarIndex, subjectType);

        v.load(tempVarIndex, subjectType);

        generateNullCheckIfNeeded();

        v.invokevirtual(
                subjectType.getInternalName(),
                "hashCode", HASH_CODE_METHOD_DESC, false
        );
    }

    @Override
    protected void generateEntries() {
        for (int hashCode : hashCodesToStringAndEntryLabel.keySet()) {
            v.visitLabel(transitionsTable.get(hashCode));

            List<Pair<String, Label>> items = hashCodesToStringAndEntryLabel.get(hashCode);
            Label nextLabel = null;

            for (int i = 0; i < items.size(); i++) {
                if (nextLabel != null) {
                    v.visitLabel(nextLabel);
                }

                Pair<String, Label> stringAndEntryLabel = items.get(i);

                v.load(tempVarIndex, subjectType);
                v.aconst(stringAndEntryLabel.first);
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
                v.goTo(stringAndEntryLabel.getSecond());
            }
        }

        super.generateEntries();
    }
}
