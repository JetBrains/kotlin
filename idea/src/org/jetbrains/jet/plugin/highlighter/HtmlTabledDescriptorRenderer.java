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

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.DeclarationDescriptor;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.*;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
* @author svtk
*/
public class HtmlTabledDescriptorRenderer extends TabledDescriptorRenderer {

    protected void renderText(TextRenderer textRenderer, StringBuilder result) {
        for (TextRenderer.TextElement element : textRenderer.elements) {
            if (element.type == TextRenderer.TextElementType.DEFAULT) {
                result.append(element.text);
            }
            else if (element.type == TextRenderer.TextElementType.ERROR) {
                result.append(error(element.text));
            }
            else if (element.type == TextRenderer.TextElementType.STRONG) {
                result.append(strong(element.text));
            }
        }
    }

    private int countRowsNumber(TableRenderer table) {
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

    @Override
    protected void renderTable(TableRenderer table, StringBuilder result) {
        if (table.rows.isEmpty()) return;
        int rowsNumber = countRowsNumber(table);


        result.append("<table>");
        for (TableRow row : table.rows) {
            result.append("<tr>");
            if (row instanceof TextRenderer) {
                StringBuilder rowText = new StringBuilder();
                renderText((TextRenderer) row, rowText);
                tdColspan(result, rowText.toString(), rowsNumber);
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

    private void renderFunctionArguments(@Nullable JetType receiverType, @NotNull List<JetType> argumentTypes, Collection<ConstraintPosition> errorPositions, StringBuilder result) {
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
            if (errorPositions.contains(ConstraintPosition.getValueParameterPosition(i))) {
                error = true;
            }
            String renderedArgument = IdeRenderers.HTML_RENDER_TYPE.render(argumentType);

            tdRight(result, strong(renderedArgument, error) + (iterator.hasNext() ? strong(",") : ""));
            i++;
        }
        td(result, strong(")"));
    }

    public static HtmlTabledDescriptorRenderer create() {
        return new HtmlTabledDescriptorRenderer();
    }

    protected HtmlTabledDescriptorRenderer() {
        super();
    }

    public static final DescriptorRenderer DESCRIPTOR_IN_TABLE = new DescriptorRenderer.HtmlDescriptorRenderer() {
        @Override
        protected boolean shouldRenderDefinedIn() {
            return false;
        }

        @Override
        protected boolean shouldRenderModifiers() {
            return false;
        }

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
