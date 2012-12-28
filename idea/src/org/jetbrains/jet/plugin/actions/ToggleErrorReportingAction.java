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

package org.jetbrains.jet.plugin.actions;

import com.intellij.codeInsight.daemon.DaemonCodeAnalyzer;
import com.intellij.openapi.actionSystem.AnActionEvent;
import com.intellij.openapi.actionSystem.ToggleAction;
import org.jetbrains.jet.plugin.highlighter.JetPsiChecker;

public class ToggleErrorReportingAction extends ToggleAction {
    @Override
    public boolean isSelected(AnActionEvent e) {
        return JetPsiChecker.isErrorReportingEnabled();
    }

    @Override
    public void setSelected(AnActionEvent e, boolean state) {
        JetPsiChecker.setErrorReportingEnabled(state);
        DaemonCodeAnalyzer.getInstance(e.getProject()).restart();
    }
}
