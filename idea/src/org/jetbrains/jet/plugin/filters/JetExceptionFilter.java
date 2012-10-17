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

package org.jetbrains.jet.plugin.filters;

import com.intellij.execution.filters.ExceptionFilter;
import com.intellij.execution.filters.Filter;
import com.intellij.execution.filters.HyperlinkInfo;
import com.intellij.psi.search.GlobalSearchScope;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class JetExceptionFilter implements Filter {
    @NotNull private final ExceptionFilter exceptionFilter;

    public JetExceptionFilter(@NotNull GlobalSearchScope searchScope) {
        exceptionFilter = new ExceptionFilter(searchScope);
    }

    @Nullable
    private HyperlinkInfo createHyperlinkInfo(@NotNull String line) {
        return null;
    }

    @NotNull
    private Result patchResult(@NotNull Result result, @NotNull String line) {
        HyperlinkInfo newHyperlinkInfo = createHyperlinkInfo(line);
        return newHyperlinkInfo == null ? result :
               new Result(result.highlightStartOffset, result.highlightEndOffset, newHyperlinkInfo, result.highlightAttributes);
    }

    @Nullable
    @Override
    public Result applyFilter(String line, int entireLength) {
        Result result = exceptionFilter.applyFilter(line, entireLength);
        return result == null ? null : patchResult(result, line);
    }
}
