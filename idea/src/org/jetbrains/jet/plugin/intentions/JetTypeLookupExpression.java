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

package org.jetbrains.jet.plugin.intentions;

import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Result;
import com.intellij.codeInsight.template.TextResult;
import com.intellij.codeInsight.template.impl.TemplateManagerImpl;
import com.intellij.codeInsight.template.impl.TemplateState;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.impl.source.tree.injected.InjectedLanguageUtil;

import java.util.Iterator;
import java.util.List;


public abstract class JetTypeLookupExpression<T> extends Expression {

    protected final LookupElement[] lookupItems;

    protected final T defaultItem;

    private final String advertisementText;

    public JetTypeLookupExpression(
            List<T> lookupItems,
            T defaultItem,
            String advertisement
    ) {
        this.advertisementText = advertisement;
        this.defaultItem = defaultItem;
        this.lookupItems = initLookupItems(lookupItems);

    }

    private LookupElement[] initLookupItems(List<T> lookupItems) {
        LookupElement[] lookupElements = new LookupElement[lookupItems.size()];
        Iterator<T> iterator = lookupItems.iterator();
        for (int i = 0; i < lookupElements.length; i++) {
            final T suggestion = iterator.next();
            lookupElements[i] = LookupElementBuilder.create(suggestion, getLookupString(suggestion)).withInsertHandler(
                    new InsertHandler<LookupElement>() {
                        @Override
                        public void handleInsert(InsertionContext context, LookupElement item) {
                            Editor topLevelEditor = InjectedLanguageUtil.getTopLevelEditor(context.getEditor());
                            TemplateState templateState = TemplateManagerImpl.getTemplateState(topLevelEditor);
                            if (templateState != null) {
                                TextRange range = templateState.getCurrentVariableRange();
                                if (range != null) {
                                    topLevelEditor.getDocument()
                                            .replaceString(range.getStartOffset(), range.getEndOffset(), getResult((T) item.getObject()));
                                }
                            }
                        }
                    });
        }
        return lookupElements;
    }

    public LookupElement[] calculateLookupItems(ExpressionContext context) {
        return lookupItems.length > 1 ? lookupItems : null;
    }

    public Result calculateQuickResult(ExpressionContext context) {
        return calculateResult(context);
    }

    public Result calculateResult(ExpressionContext context) {
        return new TextResult(getLookupString(defaultItem));
    }

    @Override
    public String getAdvertisingText() {
        return advertisementText;
    }

    protected abstract String getLookupString(T element);

    protected abstract String getResult(T element);
}
