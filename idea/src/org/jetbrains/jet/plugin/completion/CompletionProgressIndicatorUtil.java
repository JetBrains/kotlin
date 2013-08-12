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

package org.jetbrains.jet.plugin.completion;

import com.intellij.codeInsight.completion.CompletionProgressIndicator;
import com.intellij.codeInsight.completion.CompletionService;
import com.intellij.openapi.progress.ProcessCanceledException;

public class CompletionProgressIndicatorUtil {
    private CompletionProgressIndicatorUtil() {
    }

    public static ProcessCanceledException rethrowWithCancelIndicator(ProcessCanceledException exception) {
        CompletionProgressIndicator indicator = (CompletionProgressIndicator) CompletionService.getCompletionService().getCurrentCompletion();
        assert indicator != null;

        // Force cancel to avoid deadlock in CompletionThreading.delegateWeighing()
        if (!indicator.isCanceled()) {
            indicator.cancel();
        }

        return exception;
    }
}
