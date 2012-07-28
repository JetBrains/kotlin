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

package org.jetbrains.jet.lang.diagnostics.rendering;

import com.google.common.base.Predicate;
import com.google.common.base.Predicates;
import com.google.common.collect.Lists;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.CallableDescriptor;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TableRenderer.*;
import org.jetbrains.jet.lang.diagnostics.rendering.TabledDescriptorRenderer.TextRenderer.TextElement;
import org.jetbrains.jet.lang.resolve.calls.inference.ConstraintPosition;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.resolve.DescriptorRenderer;

import java.util.Iterator;
import java.util.List;

/**
 * @author svtk
 */
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
            public final JetType receiverType;
            public final List<JetType> argumentTypes;
            public final Predicate<ConstraintPosition> isErrorPosition;

            public FunctionArgumentsRow(JetType receiverType, List<JetType> argumentTypes, Predicate<ConstraintPosition> isErrorPosition) {
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

        public TableRenderer functionArgumentTypeList(@Nullable JetType receiverType, @NotNull List<JetType> argumentTypes) {

            return functionArgumentTypeList(receiverType, argumentTypes, Predicates.<ConstraintPosition>alwaysFalse());
        }

        public TableRenderer functionArgumentTypeList(@Nullable JetType receiverType,
                @NotNull List<JetType> argumentTypes,
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


    //protected final List<TableRenderer> previousTables = Lists.newArrayList();
    //private TextRow currentFirstText;
    //private List<TableRow> currentRows;
    //
    //public TabledDescriptorRenderer newElement() {
    //    previousTables.add(new TableRenderer(currentFirstText, currentRows));
    //    currentFirstText = null;
    //    currentRows = Lists.newArrayList();
    //    return this;
    //}
    //
    //public TabledDescriptorRenderer text(String text) {
    //    if (currentRows.isEmpty()) {
    //        currentFirstText = new TextRow(text);
    //        return this;
    //    }
    //    currentRows.add(new TextRow(text));
    //    return this;
    //}
    //
    //public TabledDescriptorRenderer text(String text, Object... args) {
    //    return text(String.format(text, args));
    //}




    //private TabledDescriptorRenderer(@Nullable TextRow firstText, @NotNull List<TableRow> rows) {
    //    this.currentFirstText = firstText;
    //    this.currentRows = rows;
    //}
    //
    //protected TabledDescriptorRenderer() {
    //    this(null, Lists.<TableRow>newArrayList());
    //}

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

    protected void renderText(TextRenderer textRenderer, StringBuilder result) {
        for (TextElement element : textRenderer.elements) {
            result.append(element.text);
        }
    }

    protected void renderTable(TableRenderer table, StringBuilder result) {
        if (table.rows.isEmpty()) return;
        for (TableRow row : table.rows) {
            if (row instanceof TextRenderer) {
                renderText((TextRenderer) row, result);
            }
            if (row instanceof DescriptorRow) {
                result.append(DescriptorRenderer.COMPACT.render(((DescriptorRow) row).descriptor));
            }
            if (row instanceof FunctionArgumentsRow) {
                FunctionArgumentsRow functionArgumentsRow = (FunctionArgumentsRow) row;
                renderFunctionArguments(functionArgumentsRow.receiverType, functionArgumentsRow.argumentTypes, result);
            }
            result.append("\n");
        }
    }

    private void renderFunctionArguments(@Nullable JetType receiverType, @NotNull List<JetType> argumentTypes, StringBuilder result) {
        boolean hasReceiver = receiverType != null;
        if (hasReceiver) {
            result.append("receiver: ");
            result.append(Renderers.RENDER_TYPE.render(receiverType));
            result.append("  arguments: ");
        }
        if (argumentTypes.isEmpty()) {
            result.append("()");
            return;
        }

        result.append("(");
        for (Iterator<JetType> iterator = argumentTypes.iterator(); iterator.hasNext(); ) {
            JetType argumentType = iterator.next();
            String renderedArgument = Renderers.RENDER_TYPE.render(argumentType);

            result.append(renderedArgument);
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
}
