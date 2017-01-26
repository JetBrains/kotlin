/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.highlighter;

import com.google.common.base.Predicate;
import kotlin.Unit;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor;
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.DiagnosticParameterRenderer;
import org.jetbrains.kotlin.diagnostics.rendering.RenderingContext;
import org.jetbrains.kotlin.diagnostics.rendering.SmartDescriptorRenderer;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.DescriptorRow;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.FunctionArgumentsRow;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.TableRow;
import org.jetbrains.kotlin.idea.highlighter.renderersUtil.RenderersUtilKt;
import org.jetbrains.kotlin.renderer.DescriptorRenderer;
import org.jetbrains.kotlin.renderer.DescriptorRendererModifier;
import org.jetbrains.kotlin.renderer.DescriptorRendererOptions;
import org.jetbrains.kotlin.renderer.RenderingFormat;
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition;
import org.jetbrains.kotlin.types.KotlinType;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.RECEIVER_POSITION;
import static org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPositionKind.VALUE_PARAMETER_POSITION;

public class HtmlTabledDescriptorRenderer extends TabledDescriptorRenderer {


    @NotNull
    @Override
    public DiagnosticParameterRenderer<KotlinType> getTypeRenderer() {
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
            result.append(RenderersUtilKt.renderError(text));
        }
        else if (elementType == TextElementType.STRONG) {
            result.append(RenderersUtilKt.renderStrong(text));
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

        RenderingContext context = computeRenderingContext(table);

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
                tdRightBoldColspan(result, 2, DESCRIPTOR_IN_TABLE.render(((DescriptorRow) row).descriptor, context));
            }
            if (row instanceof FunctionArgumentsRow) {
                FunctionArgumentsRow functionArgumentsRow = (FunctionArgumentsRow) row;
                renderFunctionArguments(functionArgumentsRow.receiverType, functionArgumentsRow.argumentTypes, functionArgumentsRow.isErrorPosition, result, context);
            }
            result.append("</tr>");
        }


        result.append("</table>");
    }

    private void renderFunctionArguments(
            @Nullable KotlinType receiverType,
            @NotNull List<KotlinType> argumentTypes,
            Predicate<ConstraintPosition> isErrorPosition,
            StringBuilder result,
            @NotNull RenderingContext context
    ) {
        boolean hasReceiver = receiverType != null;
        tdSpace(result);
        String receiver = "";
        if (hasReceiver) {
            boolean error = false;
            if (isErrorPosition.apply(RECEIVER_POSITION.position())) {
                error = true;
            }
            receiver = "receiver: " + RenderersUtilKt.renderStrong(getTypeRenderer().render(receiverType, context), error);
        }
        td(result, receiver);
        td(result, hasReceiver ? "arguments: " : "");
        if (argumentTypes.isEmpty()) {
            tdBold(result, "( )");
            return;
        }

        td(result, RenderersUtilKt.renderStrong("("));
        int i = 0;
        for (Iterator<KotlinType> iterator = argumentTypes.iterator(); iterator.hasNext(); ) {
            KotlinType argumentType = iterator.next();
            boolean error = false;
            if (isErrorPosition.apply(VALUE_PARAMETER_POSITION.position(i))) {
                error = true;
            }
            String renderedArgument = argumentType == null ? "unknown" : getTypeRenderer().render(argumentType, context);

            tdRight(result, RenderersUtilKt.renderStrong(renderedArgument, error) + (iterator.hasNext() ? RenderersUtilKt.renderStrong(",") : ""));
            i++;
        }
        td(result, RenderersUtilKt.renderStrong(")"));
    }

    public static HtmlTabledDescriptorRenderer create() {
        return new HtmlTabledDescriptorRenderer();
    }

    protected HtmlTabledDescriptorRenderer() {
        super();
    }

    private static final DescriptorRenderer.ValueParametersHandler VALUE_PARAMETERS_HANDLER = new DescriptorRenderer.ValueParametersHandler() {
        @Override
        public void appendBeforeValueParameter(
                @NotNull ValueParameterDescriptor parameter, int parameterIndex, int parameterCount, @NotNull StringBuilder builder
        ) {
            builder.append("<td align=\"right\" style=\"white-space:nowrap;font-weight:bold;\">");
        }

        @Override
        public void appendAfterValueParameter(
                @NotNull ValueParameterDescriptor parameter, int parameterIndex, int parameterCount, @NotNull StringBuilder builder
        ) {
            boolean last = parameterIndex == parameterCount - 1;
            if (!last) {
                builder.append(",");
            }
            builder.append("</td>");
        }

        @Override
        public void appendBeforeValueParameters(int parameterCount, @NotNull StringBuilder builder) {
            builder.append("</td>");
            if (parameterCount == 0) {
                tdBold(builder, "( )");
            }
            else {
                tdBold(builder, "(");
            }
        }

        @Override
        public void appendAfterValueParameters(int parameterCount, @NotNull StringBuilder builder) {
            if (parameterCount != 0) {
                tdBold(builder, ")");
            }
            builder.append("<td style=\"white-space:nowrap;font-weight:bold;\">");
        }
    };

    private static final DiagnosticParameterRenderer<DeclarationDescriptor>
            DESCRIPTOR_IN_TABLE = new SmartDescriptorRenderer(DescriptorRenderer.Companion.withOptions(
            new Function1<DescriptorRendererOptions, Unit>() {
                @Override
                public Unit invoke(DescriptorRendererOptions options) {
                    options.setWithDefinedIn(false);
                    options.setModifiers(Collections.<DescriptorRendererModifier>emptySet());
                    options.setValueParametersHandler(VALUE_PARAMETERS_HANDLER);
                    options.setTextFormat(RenderingFormat.HTML);
                    return Unit.INSTANCE;
                }
            }));

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
