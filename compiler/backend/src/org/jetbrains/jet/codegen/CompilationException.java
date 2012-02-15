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

package org.jetbrains.jet.codegen;

import com.intellij.openapi.editor.Document;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;

/**
* @author alex.tkachman
*/
public class CompilationException extends RuntimeException {
    private PsiElement element;

    CompilationException(String message, Throwable cause, PsiElement element) {
        super(message, cause);
        this.element = element;
    }

    public PsiElement getElement() {
        return element;
    }

    @Override
    public String toString() {
        PsiFile psiFile = element.getContainingFile();
        TextRange textRange = element.getTextRange();
        Document document = psiFile.getViewProvider().getDocument();
        int line;
        int col;
        if (document != null) {
            line = document.getLineNumber(textRange.getStartOffset());
            col = textRange.getStartOffset() - document.getLineStartOffset(line) + 1;
        }
        else {
            line = -1;
            col = -1;
        }

        String s2 = getCause().getMessage() != null ? getCause().getMessage() : getCause().toString();
        return "Internal error: (" + (line+1) + "," + col + ") " + s2 + "\n@" + where();
    }
    
    private String where() {
        if (getCause().getStackTrace().length > 0) {
            return getCause().getStackTrace()[0].getFileName() + ":" + getCause().getStackTrace()[0].getLineNumber();
        } else {
            return "far away in cyberspace";
        }
    }

    @Override
    public String getMessage() {
        return this.toString();
    }
}
