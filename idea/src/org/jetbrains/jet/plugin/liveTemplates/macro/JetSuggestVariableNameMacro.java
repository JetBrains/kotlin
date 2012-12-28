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

package org.jetbrains.jet.plugin.liveTemplates.macro;

import com.intellij.codeInsight.template.Expression;
import com.intellij.codeInsight.template.ExpressionContext;
import com.intellij.codeInsight.template.Macro;
import com.intellij.codeInsight.template.Result;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.plugin.JetBundle;

public class JetSuggestVariableNameMacro extends Macro {
    @Override
    public String getName() {
        return "kotlinSuggestVariableName";
    }

    @Override
    public String getPresentableName() {
        return JetBundle.message("macro.suggest.variable.name");
    }

    @Override
    public Result calculateResult(@NotNull Expression[] params, ExpressionContext context) {
        return null;  //TODO
    }
}
