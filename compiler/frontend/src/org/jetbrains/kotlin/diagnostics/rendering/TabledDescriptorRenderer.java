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

package org.jetbrains.kotlin.diagnostics.rendering;

import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.descriptors.CallableDescriptor;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.DescriptorRow;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.FunctionArgumentsRow;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.TableRow;
import org.jetbrains.kotlin.diagnostics.rendering.TabledDescriptorRenderer.TextRenderer.TextElement;
import org.jetbrains.kotlin.resolve.calls.inference.constraintPosition.ConstraintPosition;
import org.jetbrains.kotlin.types.KotlinType;
import org.jetbrains.kotlin.types.TypeProjection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;

public class TabledDescriptorRenderer {
    public interface TableOrTextRenderer {}

    public static class TableRenderer implements TableOrTextRenderer{
        public interface TableRow {
        }

        public static class DescriptorRow implements TableRow {
            public final CallableDescriptor descriptor;

            public DescriptorRow(CallableDescriptor descriptor) {
                this.descriptor = descriptor;
            }
        }

        public static class FunctionArgumentsRow implements TableRow {
            public final KotlinType receiverType;
            public final List<KotlinType> argumentTypes;
            public final Predicate<ConstraintPosition> isErrorPosition;

            public FunctionArgumentsRow(KotlinType receiverType, List<KotlinType> argumentTypes, Predicate<ConstraintPosition> isErrorPosition) {
                this.receiverType = receiverType;
                this.argumentTypes = argumentTypes;
                this.isErrorPosition = isErrorPosition;
            }
        }

        public final List<TableRow> rows = Lists.newArrayList();

        public TableRenderer descriptor(CallableDescriptor descriptor) {
            rows.add(new DescriptorRow(descriptor));
            return this;
        }

        public TableRenderer functionArgumentTypeList(@Nullable KotlinType receiverType, @NotNull List<KotlinType> argumentTypes) {
            return functionArgumentTypeList(receiverType, argumentTypes, position -> false);
        }

        public TableRenderer functionArgumentTypeList(@Nullable KotlinType receiverType,
                @NotNull List<KotlinType> argumentTypes,
                @NotNull Predicate<ConstraintPosition> isErrorPosition) {
            rows.add(new FunctionArgumentsRow(receiverType, argumentTypes, isErrorPosition));
            return this;
        }

        public TableRenderer text(@NotNull String text) {
            rows.add(newText().normal(text));
            return this;
        }

        public TableRenderer text(@NotNull TextRenderer textRenderer) {
            rows.add(textRenderer);
            return this;
        }
    }

    public static class TextRenderer implements TableOrTextRenderer, TableRow {
        public static class TextElement {

            public TextElementType type;
            public String text;

            public TextElement(@NotNull TextElementType type, @NotNull String text) {
                this.type = type;
                this.text = text;
            }
        }

        public final List<TextElement> elements = Lists.newArrayList();

        public TextRenderer normal(@NotNull Object text) {
            elements.add(new TextElement(TextElementType.DEFAULT, text.toString()));
            return this;
        }

        public TextRenderer error(@NotNull Object text) {
            elements.add(new TextElement(TextElementType.ERROR, text.toString()));
            return this;
        }

        public TextRenderer strong(@NotNull Object text) {
            elements.add(new TextElement(TextElementType.STRONG, text.toString()));
            return this;
        }
    }

    protected final List<TableOrTextRenderer> renderers = Lists.newArrayList();

    public TabledDescriptorRenderer text(@NotNull TextRenderer textRenderer) {
        renderers.add(textRenderer);
        return this;
    }

    public TabledDescriptorRenderer table(@NotNull TableRenderer tableRenderer) {
        renderers.add(tableRenderer);
        return this;
    }

    public static TextRenderer newText() {
        return new TextRenderer();
    }

    public static TableRenderer newTable() {
        return new TableRenderer();
    }

    @Override
    public String toString() {
        StringBuilder result = new StringBuilder();
        for (TableOrTextRenderer tableOrTextRenderer : renderers) {
            if (tableOrTextRenderer instanceof TableRenderer) {
                renderTable((TableRenderer)tableOrTextRenderer, result);
            }
            else {
                renderText((TextRenderer)tableOrTextRenderer, result);
            }
        }
        return result.toString();
    }

    @NotNull
    public DiagnosticParameterRenderer<KotlinType> getTypeRenderer() {
        return Renderers.RENDER_TYPE;
    }

    @NotNull
    public DiagnosticParameterRenderer<TypeProjection> getTypeProjectionRenderer() {
        return Renderers.TYPE_PROJECTION;
    }

    protected void renderText(TextRenderer textRenderer, StringBuilder result) {
        for (TextElement element : textRenderer.elements) {
            result.append(element.text);
        }
    }

    protected void renderTable(TableRenderer table, StringBuilder result) {
        if (table.rows.isEmpty()) return;

        RenderingContext context = computeRenderingContext(table);
        for (TableRow row : table.rows) {
            if (row instanceof TextRenderer) {
                renderText((TextRenderer) row, result);
            }
            if (row instanceof DescriptorRow) {
                result.append(Renderers.COMPACT.render(((DescriptorRow) row).descriptor, context));
            }
            if (row instanceof FunctionArgumentsRow) {
                FunctionArgumentsRow functionArgumentsRow = (FunctionArgumentsRow) row;
                renderFunctionArguments(functionArgumentsRow.receiverType, functionArgumentsRow.argumentTypes, result, context);
            }
            result.append("\n");
        }
    }

    private void renderFunctionArguments(
            @Nullable KotlinType receiverType,
            @NotNull List<KotlinType> argumentTypes,
            StringBuilder result,
            @NotNull RenderingContext context
    ) {
        boolean hasReceiver = receiverType != null;
        if (hasReceiver) {
            result.append("receiver: ");
            result.append(getTypeRenderer().render(receiverType, context));
            result.append("  arguments: ");
        }
        if (argumentTypes.isEmpty()) {
            result.append("()");
            return;
        }

        result.append("(");
        for (Iterator<KotlinType> iterator = argumentTypes.iterator(); iterator.hasNext(); ) {
            KotlinType argumentType = iterator.next();
            if (argumentType == null) {
                result.append("<unknown>");
            }
            else {
                String renderedArgument = getTypeRenderer().render(argumentType, context);
                result.append(renderedArgument);
            }

            if (iterator.hasNext()) {
                result.append(",");
            }
        }
        result.append(")");
    }

    public static TabledDescriptorRenderer create() {
        return new TabledDescriptorRenderer();
    }

    public static enum TextElementType { STRONG, ERROR, DEFAULT }

    @NotNull
    protected static RenderingContext computeRenderingContext(@NotNull TableRenderer table) {
        ArrayList<Object> toRender = new ArrayList<>();
        for (TableRow row : table.rows) {
            if (row instanceof DescriptorRow) {
                toRender.add(((DescriptorRow) row).descriptor);
            }
            else if (row instanceof FunctionArgumentsRow) {
                toRender.add(((FunctionArgumentsRow) row).receiverType);
                toRender.addAll(((FunctionArgumentsRow) row).argumentTypes);
            }
            else if (row instanceof TextRenderer) {

            }
            else {
                throw new AssertionError("Unknown row of type " + row.getClass());
            }
        }
        return new RenderingContext.Impl(toRender);
    }
}
