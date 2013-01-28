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

package org.jetbrains.jet.plugin.parameterInfo;

import com.intellij.codeInsight.hint.HintUtil;
import com.intellij.lang.parameterInfo.ParameterInfoUIContext;
import com.intellij.psi.PsiElement;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collections;

/**
 * User: Alexander Podkhalyuzin
 * Date: 24.01.12
 */
public class MockParameterInfoUIContext implements ParameterInfoUIContext {
    private PsiElement myParaeterOwner;
    private int myCurrentParameterIndex;

    private ArrayList<String> result = new ArrayList<String>();

    MockParameterInfoUIContext(PsiElement parameterOwner, int currentParameterIndex) {
        myParaeterOwner = parameterOwner;
        myCurrentParameterIndex = currentParameterIndex;
    }
    
    @Override
    public void setupUIComponentPresentation(String text, int highlightStartOffset, int highlightEndOffset,
                                             boolean isDisabled, boolean strikeout,
                                             boolean isDisabledBeforeHighlight, Color background) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Text: (").append(text).append("), Disabled: ").append(isDisabled).
                append(", Strikeout: ").append(strikeout).append(", Green: ").
                append(background.equals(JetFunctionParameterInfoHandler.GREEN_BACKGROUND));
        result.add(stringBuilder.toString());
    }

    @Override
    public boolean isUIComponentEnabled() {
        return false;
    }

    @Override
    public void setUIComponentEnabled(boolean enabled) {
    }

    @Override
    public int getCurrentParameterIndex() {
        return myCurrentParameterIndex;
    }

    @Override
    public PsiElement getParameterOwner() {
        return myParaeterOwner;
    }

    @Override
    public Color getDefaultParameterColor() {
        return HintUtil.INFORMATION_COLOR;
    }
    
    public String getResultText() {
        StringBuilder stringBuilder = new StringBuilder();
        Collections.sort(result);
        for (String s : result) {
            stringBuilder.append(s).append("\n");
        }
        return stringBuilder.toString().trim();
    }
}
