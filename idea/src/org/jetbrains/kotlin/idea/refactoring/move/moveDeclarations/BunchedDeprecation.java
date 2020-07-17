/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.refactoring.move.moveDeclarations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.search.GlobalSearchScope;
import com.intellij.refactoring.util.TextOccurrencesUtil;
import com.intellij.usageView.UsageInfo;

import java.util.Collection;

// FIX ME WHEN BUNCH 201 REMOVED
final class BunchedDeprecation {
    public static void findNonCodeUsages(
            PsiElement element,
            String stringToSearch,
            boolean searchInStringsAndComments,
            boolean searchInNonJavaFiles,
            String newQName,
            Collection<? super UsageInfo> results) {
        TextOccurrencesUtil.findNonCodeUsages(element, GlobalSearchScope.projectScope(element.getProject()),
                                              stringToSearch, searchInStringsAndComments, searchInNonJavaFiles, newQName, results);
    }
}
