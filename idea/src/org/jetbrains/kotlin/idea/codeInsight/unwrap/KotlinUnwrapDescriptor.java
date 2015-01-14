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

package org.jetbrains.kotlin.idea.codeInsight.unwrap;

import com.intellij.codeInsight.unwrap.UnwrapDescriptorBase;
import com.intellij.codeInsight.unwrap.Unwrapper;

public class KotlinUnwrapDescriptor extends UnwrapDescriptorBase {
    @Override
    protected Unwrapper[] createUnwrappers() {
        return new Unwrapper[] {
                new KotlinUnwrappers.KotlinExpressionRemover("remove.expression"),
                new KotlinUnwrappers.KotlinThenUnwrapper("unwrap.expression"),
                new KotlinUnwrappers.KotlinElseRemover("remove.else"),
                new KotlinUnwrappers.KotlinElseUnwrapper("unwrap.else"),
                new KotlinUnwrappers.KotlinLoopUnwrapper("unwrap.expression"),
                new KotlinUnwrappers.KotlinTryUnwrapper("unwrap.expression"),
                new KotlinUnwrappers.KotlinCatchUnwrapper("unwrap.expression"),
                new KotlinUnwrappers.KotlinCatchRemover("remove.expression"),
                new KotlinUnwrappers.KotlinFinallyUnwrapper("unwrap.expression"),
                new KotlinUnwrappers.KotlinFinallyRemover("remove.expression"),
                new KotlinLambdaUnwrapper("unwrap.expression"),
        };
    }
}
