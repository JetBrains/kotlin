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

package org.jetbrains.jet.plugin.completion.handlers;

import com.google.common.collect.Sets;
import com.intellij.codeInsight.TailType;
import com.intellij.codeInsight.completion.InsertHandler;
import com.intellij.codeInsight.completion.InsertionContext;
import com.intellij.codeInsight.lookup.LookupElement;

import java.util.Set;

/**
 * @author Nikolay Krasko
 */
public class JetKeywordInsertHandler implements InsertHandler<LookupElement> {

    private final static Set<String> NO_SPACE_AFTER = Sets.newHashSet("this", "super", "This", "true", "false", "null");

    @Override
    public void handleInsert(InsertionContext context, LookupElement item) {
        String keyword = item.getLookupString();

        // Add space after keyword
        if (!NO_SPACE_AFTER.contains(keyword)) {
            context.setAddCompletionChar(false);
            final TailType tailType = TailType.SPACE;
            tailType.processTail(context.getEditor(), context.getTailOffset());
        }
    }
}
