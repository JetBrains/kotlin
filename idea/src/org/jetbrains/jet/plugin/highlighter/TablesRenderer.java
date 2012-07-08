/*
 * Copyright 2010-2012 JetBrains s.r.o.
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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.*;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
* @author svtk
*/
public class TablesRenderer {
    public static class Table {
        public final TextRow firstText;
        public final List<TableRow> rows;

        public Table(TextRow firstText, List<TableRow> rows) {
            this.firstText = firstText;
            this.rows = rows;
        }
    }

    public interface TableRow {
    }

    public static class TextRow implements TableRow {
        public final String text;

        public TextRow(String text) {
            this.text = text;
        }
    }

    public static class DescriptorRow implements TableRow {
        public final CallableDescriptor descriptor;

        public DescriptorRow(CallableDescriptor descriptor) {
            this.descriptor = descriptor;
        }
    }

    public static class FunctionArgumentsRow implements TableRow {
        public final JetType receiverType;
        public final List<JetType> argumentTypes;
        public final Collection<ConstraintPosition> errorPositions;

        public FunctionArgumentsRow(JetType receiverType, List<JetType> argumentTypes, Collection<ConstraintPosition> errorPositions) {
            this.receiverType = receiverType;
            this.argumentTypes = argumentTypes;
            this.errorPositions = errorPositions;
        }
    }

    private final List<Table> previousTables = Lists.newArrayList();
    private TextRow currentFirstText;
    private List<TableRow> currentRows;

    public TablesRenderer newElement() {
        previousTables.add(new Table(currentFirstText, currentRows));
        currentFirstText = null;
        currentRows = Lists.newArrayList();
        return this;
    }

    public TablesRenderer text(String text) {
        if (currentRows.isEmpty()) {
            currentFirstText = new TextRow(text);
            return this;
        }
        currentRows.add(new TextRow(text));
        return this;
    }

    public TablesRenderer text(String text, Object... args) {
        return text(String.format(text, args));
    }

    public TablesRenderer descriptor(CallableDescriptor descriptor) {
        currentRows.add(new DescriptorRow(descriptor));
        return this;
    }

    public TablesRenderer functionArgumentTypeList(@Nullable JetType receiverType, @NotNull List<JetType> argumentTypes) {
        return functionArgumentTypeList(receiverType, argumentTypes, Collections.<ConstraintPosition>emptyList());
    }

    public TablesRenderer functionArgumentTypeList(@Nullable JetType receiverType,
            @NotNull List<JetType> argumentTypes,
            Collection<ConstraintPosition> errorPositions) {
        currentRows.add(new FunctionArgumentsRow(receiverType, argumentTypes, errorPositions));
        return this;
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        newElement();
        for (Table table : previousTables) {
            renderTable(table, result);
        }
        return result.toString();
    }

    private static int countRowsNumber(Table table) {
        int argumentsNumber = 0;
        for (TableRow row : table.rows) {
            if (row instanceof DescriptorRow) {
                int valueParametersNumber = ((DescriptorRow) row).descriptor.getValueParameters().size();
                if (valueParametersNumber > argumentsNumber) {
                    argumentsNumber = valueParametersNumber;
                }
            }
            else if (row instanceof FunctionArgumentsRow) {
                int argumentTypesNumber = ((FunctionArgumentsRow) row).argumentTypes.size();
                if (argumentTypesNumber > argumentsNumber) {
                    argumentsNumber = argumentTypesNumber;
                }
            }
        }
        //magical number 6:
        // <td> white-space </td> <td> receiver: ___ </td> <td> arguments: </td> <td> ( </td> arguments <td> ) </td> <td> : return_type </td>
        return argumentsNumber + 6;
    }



    private static void renderTable(Table table, StringBuilder result) {
        if (table.firstText == null && table.rows.isEmpty()) return;
        if (table.firstText != null) {
            result.append(table.firstText.text);
        }
        int rowsNumber = countRowsNumber(table);


        result.append("<table>");
        for (TableRow row : table.rows) {
            result.append("<tr>");
            if (row instanceof TextRow) {
                tdColspan(result, ((TextRow) row).text, rowsNumber);
            }
            if (row instanceof DescriptorRow) {
                result.append(DESCRIPTOR_IN_TABLE.render(((DescriptorRow) row).descriptor));
            }
            if (row instanceof FunctionArgumentsRow) {
                FunctionArgumentsRow functionArgumentsRow = (FunctionArgumentsRow) row;
                renderFunctionArguments(functionArgumentsRow.receiverType, functionArgumentsRow.argumentTypes, functionArgumentsRow.errorPositions, result);
            }
            result.append("</tr>");
        }


        result.append("</table>");
    }

    private static void renderFunctionArguments(@Nullable JetType receiverType, @NotNull List<JetType> argumentTypes, Collection<ConstraintPosition> errorPositions, StringBuilder result) {
        boolean hasReceiver = receiverType != null;
        tdSpace(result);
        String receiver = "";
        if (hasReceiver) {
            boolean error = false;
            if (errorPositions.contains(ConstraintPosition.RECEIVER_POSITION)) {
                error = true;
            }
            receiver = "receiver: " + strong(IdeRenderers.HTML_RENDER_TYPE.render(receiverType), error);
        }
        td(result, receiver);
        td(result, hasReceiver ? "arguments: " : "");
        if (argumentTypes.isEmpty()) {
            tdBold(result, "( )");
            return;
        }

        td(result, strong("("));
        int i = 0;
        for (Iterator<JetType> iterator = argumentTypes.iterator(); iterator.hasNext(); ) {
            JetType argumentType = iterator.next();
            boolean error = false;
            if (errorPositions.contains(ConstraintPosition.valueParameterPosition(i))) {
                error = true;
            }
            String renderedArgument = IdeRenderers.HTML_RENDER_TYPE.render(argumentType);

            tdRight(result, strong(renderedArgument, error) + (iterator.hasNext() ? strong(",") : ""));
            i++;
        }
        td(result, strong(")"));
    }

    public static TablesRenderer create() {
        return new TablesRenderer();
    }

    TablesRenderer(@Nullable TextRow firstText, @NotNull List<TableRow> rows) {
        this.currentFirstText = firstText;
        this.currentRows = rows;
    }

    TablesRenderer() {
        this(null, Lists.<TableRow>newArrayList());
    }

    public static final DescriptorRenderer DESCRIPTOR_IN_TABLE = new DescriptorRenderer.HtmlDescriptorRenderer(false, false) {
        @NotNull
        @Override
        public String render(@NotNull DeclarationDescriptor declarationDescriptor) {
            StringBuilder builder = new StringBuilder();
            tdSpace(builder);
            tdRightBoldColspan(builder, 2, super.render(declarationDescriptor));
            return builder.toString();
        }

        @Override
        protected void renderValueParameters(FunctionDescriptor descriptor, StringBuilder builder) {
            //todo comment
            builder.append("</div></td>");
            super.renderValueParameters(descriptor, builder);
            builder.append("<td><div style=\"white-space:nowrap;font-weight:bold;\">");
        }

        @Override
        protected void renderEmptyValueParameters(StringBuilder builder) {
            tdBold(builder, "( )");
        }

        @Override
        protected void renderValueParameter(ValueParameterDescriptor parameterDescriptor, boolean isLast, StringBuilder builder) {
            if (parameterDescriptor.getIndex() == 0) {
                tdBold(builder, "(");
            }
            StringBuilder parameterBuilder = new StringBuilder();
            parameterDescriptor.accept(super.subVisitor, parameterBuilder);

            tdRightBold(builder, parameterBuilder.toString() + (isLast ? "" : ","));
            if (isLast) {
                tdBold(builder, ")");
            }
        }
    };

    private static void td(StringBuilder builder, String text) {
        builder.append("<td><div style=\"white-space:nowrap;\">").append(text).append("</div></td>");
    }

    private static void tdSpace(StringBuilder builder) {
        builder.append("<td width=\"10%\"></td>");
    }

    private static void tdColspan(StringBuilder builder, String text, int colspan) {
        builder.append("<td colspan=\"").append(colspan).append("\"><div style=\"white-space:nowrap;\">").append(text).append("</div></td>");
    }

    private static void tdBold(StringBuilder builder, String text) {
        builder.append("<td><div style=\"white-space:nowrap;font-weight:bold;\">").append(text).append("</div></td>");
    }

    private static void tdRight(StringBuilder builder, String text) {
        builder.append("<td align=\"right\"><div style=\"white-space:nowrap;\">").append(text).append("</div></td>");
    }

    private static void tdRightBold(StringBuilder builder, String text) {
        builder.append("<td align=\"right\"><div style=\"white-space:nowrap;font-weight:bold;\">").append(text).append("</div></td>");
    }

    private static void tdRightBoldColspan(StringBuilder builder, int colspan, String text) {
        builder.append("<td align=\"right\" colspan=\"").append(colspan).append("\"><div style=\"white-space:nowrap;font-weight:bold;\">").append(text).append("</div></td>");
    }
}
