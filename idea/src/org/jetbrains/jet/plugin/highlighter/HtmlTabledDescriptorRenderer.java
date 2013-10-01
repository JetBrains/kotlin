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

package org.jetbrains.jet.plugin.highlighter;

import com.google.common.base.Predicate;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.FunctionDescriptor;
import org.jetbrains.jet.lang.descriptors.ValueParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.DescriptorRow;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.FunctionArgumentsRow;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.TableRow;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.renderer.DescriptorRenderer;
import org.jetbrains.jet.renderer.DescriptorRendererBuilder;
import org.jetbrains.jet.renderer.Renderer;

import java.util.Iterator;
import java.util.List;

import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.error;
import static org.jetbrains.jet.plugin.highlighter.IdeRenderers.strong;

public class HtmlTabledDescriptorRenderer extends TabledDescriptorRenderer {


    @NotNull
    @Override
    public Renderer<JetType> getTypeRenderer() {
        return IdeRenderers.HTML_RENDER_TYPE;
    }

    @Override
    protected void renderText(TextRenderer textRenderer, StringBuilder result) {
        for (TextRenderer.TextElement element : textRenderer.elements) {
            renderText(result, element.type, element.text);
        }
    }

    private static void renderText(StringBuilder result, TextElementType elementType, String text) {
        if (elementType == TextElementType.DEFAULT) {
            result.append(text);
        }
        else if (elementType == TextElementType.ERROR) {
            result.append(error(text));
        }
        else if (elementType == TextElementType.STRONG) {
            result.append(strong(text));
        }
    }

    private static int countColumnNumber(TableRenderer table) {
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
        int rowsNumber = countColumnNumber(table);


        result.append("<table>");
        for (TableRow row : table.rows) {
            result.append("<tr>");
            if (row instanceof TextRenderer) {
                StringBuilder rowText = new StringBuilder();
                renderText((TextRenderer) row, rowText);
                tdColspan(result, rowText.toString(), rowsNumber);
            }
            if (row instanceof DescriptorRow) {
                tdSpace(result);
                tdRightBoldColspan(result, 2, DESCRIPTOR_IN_TABLE.render(((DescriptorRow) row).descriptor));
            }
            if (row instanceof FunctionArgumentsRow) {
                FunctionArgumentsRow functionArgumentsRow = (FunctionArgumentsRow) row;
                renderFunctionArguments(functionArgumentsRow.receiverType, functionArgumentsRow.argumentTypes, functionArgumentsRow.isErrorPosition, result);
            }
            result.append("</tr>");
        }


        result.append("</table>");
    }

    private void renderFunctionArguments(
            @Nullable JetType receiverType,
            @NotNull List<JetType> argumentTypes,
            Predicate<ConstraintPosition> isErrorPosition,
            StringBuilder result
    ) {
        boolean hasReceiver = receiverType != null;
        tdSpace(result);
        String receiver = "";
        if (hasReceiver) {
            boolean error = false;
            if (isErrorPosition.apply(ConstraintPosition.RECEIVER_POSITION)) {
                error = true;
            }
            receiver = "receiver: " + strong(getTypeRenderer().render(receiverType), error);
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
            if (isErrorPosition.apply(ConstraintPosition.getValueParameterPosition(i))) {
                error = true;
            }
            String renderedArgument = getTypeRenderer().render(argumentType);

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

    private static final DescriptorRenderer.ValueParametersHandler VALUE_PARAMETERS_HANDLER = new DescriptorRenderer.ValueParametersHandler() {
        @Override
        public void appendBeforeValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder) {
            stringBuilder.append("<td align=\"right\" style=\"white-space:nowrap;font-weight:bold;\">");
        }

        @Override
        public void appendAfterValueParameter(@NotNull ValueParameterDescriptor parameter, @NotNull StringBuilder stringBuilder) {
            boolean last = ((FunctionDescriptor) parameter.getContainingDeclaration()).getValueParameters().size() - 1 == parameter.getIndex();
            if (!last) {
                stringBuilder.append(",");
            }
            stringBuilder.append("</td>");
        }

        @Override
        public void appendBeforeValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder) {
            stringBuilder.append("</td>");
            if (function.getValueParameters().isEmpty()) {
                tdBold(stringBuilder, "( )");
            }
            else {
                tdBold(stringBuilder, "(");
            }
        }

        @Override
        public void appendAfterValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder stringBuilder) {
            if (!function.getValueParameters().isEmpty()) {
                tdBold(stringBuilder, ")");
            }
            stringBuilder.append("<td style=\"white-space:nowrap;font-weight:bold;\">");
        }
    };

    public static final DescriptorRenderer DESCRIPTOR_IN_TABLE = new DescriptorRendererBuilder()
            .setWithDefinedIn(false)
            .setModifiers()
            .setValueParametersHandler(VALUE_PARAMETERS_HANDLER)
            .setTextFormat(DescriptorRenderer.TextFormat.HTML).build();

    private static void td(StringBuilder builder, String text) {
        builder.append("<td style=\"white-space:nowrap;\">").append(text).append("</td>");
    }

    private static void tdSpace(StringBuilder builder) {
        builder.append("<td width=\"10%\"></td>");
    }

    private static void tdColspan(StringBuilder builder, String text, int colspan) {
        builder.append("<td colspan=\"").append(colspan).append("\" style=\"white-space:nowrap;\">").append(text).append("</td>");
    }

    private static void tdBold(StringBuilder builder, String text) {
        builder.append("<td style=\"white-space:nowrap;font-weight:bold;\">").append(text).append("</td>");
    }

    private static void tdRight(StringBuilder builder, String text) {
        builder.append("<td align=\"right\" style=\"white-space:nowrap;\">").append(text).append("</td>");
    }

    private static void tdRightBoldColspan(StringBuilder builder, int colspan, String text) {
        builder.append("<td align=\"right\" colspan=\"").append(colspan).append("\" style=\"white-space:nowrap;font-weight:bold;\">").append(text).append("</td>");
    }

    public static String tableForTypes(String message, String firstDescription, TextElementType firstType, String secondDescription, TextElementType secondType) {
        StringBuilder result = new StringBuilder();
        result.append("<html>").append(message);
        result.append("<table><tr><td>").append(firstDescription).append("</td><td>");
        renderText(result, firstType, "{0}");
        result.append("</td></tr><tr><td>").append(secondDescription).append("</td><td>");
        renderText(result, secondType, "{1}");
        result.append("</td></tr></table></html>");
        return result.toString();
    }
}
