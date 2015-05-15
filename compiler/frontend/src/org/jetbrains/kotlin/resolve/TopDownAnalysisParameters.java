/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.resolve;

import com.google.common.base.Predicate;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.context.GlobalContext;
import org.jetbrains.kotlin.context.TypeLazinessToken;
import org.jetbrains.kotlin.storage.ExceptionTracker;
import org.jetbrains.kotlin.storage.StorageManager;

/**
 * Various junk that cannot be placed into context (yet).
 */
public class TopDownAnalysisParameters extends TypeLazinessToken {

    @NotNull
    public static TopDownAnalysisParameters create(
            boolean declaredLocally
    ) {
        return new TopDownAnalysisParameters(declaredLocally, true);
    }

    @NotNull
    public static TopDownAnalysisParameters createForLocalDeclarations() {
        return new TopDownAnalysisParameters(true, false);
    }

    private final boolean declaredLocally;
    private final boolean lazyTopDownAnalysis;

    private TopDownAnalysisParameters(
            boolean declaredLocally,
            boolean lazyTopDownAnalysis
    ) {
        this.declaredLocally = declaredLocally;
        this.lazyTopDownAnalysis = lazyTopDownAnalysis;
    }

    public boolean isDeclaredLocally() {
        return declaredLocally;
    }

    // Used temporarily while we are transitioning from eager to lazy analysis of headers in the IDE
    @Override
    @Deprecated
    public boolean isLazy() {
        return lazyTopDownAnalysis;
    }
}
