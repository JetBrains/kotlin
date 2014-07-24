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
import org.jetbrains.jet.codegen.FrameMap;
import org.jetbrains.jet.lang.psi.JetWhenEntry;
import org.jetbrains.jet.lang.psi.JetWhenExpression;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.constants.CompileTimeConstant;
import org.jetbrains.jet.lang.resolve.constants.NullValue;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.org.objectweb.asm.Label;
import org.jetbrains.org.objectweb.asm.Type;
import org.jetbrains.org.objectweb.asm.commons.InstructionAdapter;

import java.util.*;

import static org.jetbrains.jet.lang.resolve.BindingContext.EXPRESSION_TYPE;

abstract public class SwitchCodegen {
    protected final JetWhenExpression expression;
    protected final boolean isStatement;
    protected final ExpressionCodegen codegen;
    protected final BindingContext bindingContext;
    protected final Type subjectType;
    protected final Type resultType;
    protected final InstructionAdapter v;

    protected final NavigableMap<Integer, Label> transitionsTable = new TreeMap<Integer, Label>();
    protected final List<Label> entryLabels = new ArrayList<Label>();
    protected Label elseLabel = new Label();
    protected Label endLabel = new Label();
    protected Label defaultLabel;

    public SwitchCodegen(
            @NotNull JetWhenExpression expression, boolean isStatement,
            @NotNull ExpressionCodegen codegen
    ) {
        this.expression = expression;
        this.isStatement = isStatement;
        this.codegen = codegen;
        this.bindingContext = codegen.getBindingContext();

        subjectType = codegen.expressionType(expression.getSubjectExpression());
        resultType = isStatement ? Type.VOID_TYPE : codegen.expressionType(expression);
        v = codegen.v;
    }

    /**
     * Generates bytecode for entire when expression
     */
    public void generate() {
        prepareConfiguration();

        boolean hasElse = expression.getElseExpression() != null;

        // if there is no else-entry and it's statement then default --- endLabel
        defaultLabel = (hasElse || !isStatement) ? elseLabel : endLabel;

        generateSubject();

        generateSwitchInstructionByTransitionsTable();

        generateEntries();

        // there is no else-entry but this is not statement, so we should return Unit
        if (!hasElse && !isStatement) {
            v.visitLabel(elseLabel);
            codegen.putUnitInstanceOntoStackForNonExhaustiveWhen(expression);
        }

        codegen.markLineNumber(expression);
        v.mark(endLabel);
    }

    /**
     * Sets up transitionsTable and maybe something else needed in a special case
     * Behaviour may be changed by overriding processConstant
     */
    private void prepareConfiguration() {
        for (JetWhenEntry entry : expression.getEntries()) {
            Label entryLabel = new Label();

            for (CompileTimeConstant constant : SwitchCodegenUtil.getConstantsFromEntry(entry, bindingContext)) {
                if (constant instanceof NullValue) continue;
                processConstant(constant, entryLabel);
            }

            if (entry.isElse()) {
                elseLabel = entryLabel;
            }

            entryLabels.add(entryLabel);
        }
    }

    abstract protected void processConstant(
            @NotNull CompileTimeConstant constant,
            @NotNull Label entryLabel
    );

    protected void putTransitionOnce(int value, @NotNull Label entryLabel) {
        if (!transitionsTable.containsKey(value)) {
            transitionsTable.put(value, entryLabel);
        }
    }

    /**
     * Should generate int subject on top of the stack
     * Default implementation just run codegen for actual subject of expression
     * May also gen nullability check if needed
     */
    protected void generateSubject() {
        codegen.gen(expression.getSubjectExpression(), subjectType);
    }

    protected void generateNullCheckIfNeeded() {
        JetType subjectJetType = bindingContext.get(EXPRESSION_TYPE, expression.getSubjectExpression());

        assert subjectJetType != null : "subject type can't be null (i.e. void)";

        if (subjectJetType.isNullable()) {
            int nullEntryIndex = findNullEntryIndex(expression);
            Label nullLabel = nullEntryIndex == -1 ? defaultLabel : entryLabels.get(nullEntryIndex);
            Label notNullLabel = new Label();

            v.dup();
            v.ifnonnull(notNullLabel);

            v.pop();

            v.goTo(nullLabel);

            v.visitLabel(notNullLabel);
        }
    }

    private int findNullEntryIndex(@NotNull JetWhenExpression expression) {
        int entryIndex = 0;
        for (JetWhenEntry entry : expression.getEntries()) {
            for (CompileTimeConstant constant : SwitchCodegenUtil.getConstantsFromEntry(entry, bindingContext)) {
                if (constant instanceof NullValue) {
                    return entryIndex;
                }
            }

            entryIndex++;
        }

        return -1;
    }

    private void generateSwitchInstructionByTransitionsTable() {
        int[] keys = new int[transitionsTable.size()];
        Label[] labels = new Label[transitionsTable.size()];
        int i = 0;

        for (Map.Entry<Integer, Label> transition : transitionsTable.entrySet()) {
            keys[i] = transition.getKey();
            labels[i] = transition.getValue();

            i++;
        }

        int nlabels = keys.length;
        int hi = keys[nlabels - 1];
        int lo = keys[0];

        /*
         * Heuristic estimation if it's better to use tableswitch or lookupswitch.
         * From OpenJDK sources
         */
        long table_space_cost = 4 + ((long) hi - lo + 1); // words
        long table_time_cost = 3; // comparisons
        long lookup_space_cost = 3 + 2 * (long) nlabels;
        long lookup_time_cost = nlabels;

        boolean useTableSwitch = nlabels > 0 &&
                                 table_space_cost + 3 * table_time_cost <=
                                 lookup_space_cost + 3 * lookup_time_cost;

        if (!useTableSwitch) {
            v.lookupswitch(defaultLabel, keys, labels);
            return;
        }

        Label[] sparseLabels = new Label[hi - lo + 1];
        Arrays.fill(sparseLabels, defaultLabel);

        for (i = 0; i < keys.length; i++) {
            sparseLabels[keys[i] - lo] = labels[i];
        }

        v.tableswitch(lo, hi, defaultLabel, sparseLabels);
    }

    protected void generateEntries() {
        // resolving entries' entryLabels and generating entries' code
        Iterator<Label> entryLabelsIterator = entryLabels.iterator();
        for (JetWhenEntry entry : expression.getEntries()) {
            v.visitLabel(entryLabelsIterator.next());

            FrameMap.Mark mark = codegen.myFrameMap.mark();
            codegen.gen(entry.getExpression(), resultType);
            mark.dropTo();

            if (!entry.isElse()) {
                v.goTo(endLabel);
            }
        }
    }
}
