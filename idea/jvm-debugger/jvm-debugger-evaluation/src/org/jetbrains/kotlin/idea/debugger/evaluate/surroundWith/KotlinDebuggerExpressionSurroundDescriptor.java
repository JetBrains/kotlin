/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.idea.debugger.evaluate.surroundWith;

import com.intellij.lang.surroundWith.Surrounder;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.kotlin.idea.core.surroundWith.KotlinExpressionSurroundDescriptorBase;
import org.jetbrains.kotlin.idea.debugger.surroundWith.KotlinRuntimeTypeCastSurrounder;

public class KotlinDebuggerExpressionSurroundDescriptor extends KotlinExpressionSurroundDescriptorBase {

    private static final Surrounder[] SURROUNDERS = {
            new KotlinRuntimeTypeCastSurrounder()
    };

    @Override
    @NotNull
    public Surrounder[] getSurrounders() {
        return SURROUNDERS;
    }
}
