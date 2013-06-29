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

package org.jetbrains.jet.plugin;

import com.intellij.codeInsight.folding.JavaCodeFoldingSettings;
import com.intellij.lang.ASTNode;
import com.intellij.lang.folding.FoldingBuilderEx;
import com.intellij.lang.folding.FoldingDescriptor;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.DumbAware;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiWhiteSpace;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.kdoc.lexer.KDocTokens;
import org.jetbrains.jet.lang.psi.JetFile;
import org.jetbrains.jet.lang.psi.JetImportDirective;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetFoldingBuilder extends FoldingBuilderEx implements DumbAware {
    @NotNull
    @Override
    public FoldingDescriptor[] buildFoldRegions(@NotNull PsiElement root, @NotNull Document document, boolean quick) {
        if (!(root instanceof JetFile)) {
            return FoldingDescriptor.EMPTY;
        }
        List<FoldingDescriptor> descriptors = new ArrayList<FoldingDescriptor>();
        JetFile file = (JetFile) root;

        List<JetImportDirective> importList = file.getImportDirectives();
        if (importList != null && importList.size() > 1) {
            JetImportDirective firstImport = importList.get(0);
            PsiElement importKeyword = firstImport.getFirstChild();
            int startOffset = importKeyword.getTextRange().getEndOffset() + 1;
            int endOffset = importList.get(importList.size() - 1).getTextRange().getEndOffset();
            TextRange range = new TextRange(startOffset, endOffset);
            descriptors.add(new FoldingDescriptor(firstImport, range));
        }

        appendDescriptors(root.getNode(), document, descriptors);
        return descriptors.toArray(new FoldingDescriptor[descriptors.size()]);
    }

    private static void appendDescriptors(ASTNode node, Document document, List<FoldingDescriptor> descriptors) {
        TextRange textRange = node.getTextRange();
        IElementType type = node.getElementType();
        if ((type == JetNodeTypes.BLOCK || type == JetNodeTypes.CLASS_BODY || type == JetTokens.BLOCK_COMMENT || type == KDocTokens.KDOC) &&
                !isOneLine(textRange, document)) {
            descriptors.add(new FoldingDescriptor(node, textRange));
        }
        else if (node.getElementType() == JetTokens.IDE_TEMPLATE_START) {
            ASTNode next = node.getTreeNext();
            if (next != null) {
                ASTNode nextNext = next.getTreeNext();
                if (nextNext != null && nextNext.getElementType() == JetTokens.IDE_TEMPLATE_END) {
                    TextRange range = new TextRange(node.getStartOffset(), nextNext.getStartOffset() + nextNext.getTextLength());
                    descriptors.add(new FoldingDescriptor(next, range, null, Collections.<Object>emptySet(), true));
                }
            }
        }
        ASTNode child = node.getFirstChildNode();
        while (child != null) {
          appendDescriptors(child, document, descriptors);
          child = child.getTreeNext();
        }
    }

    private static boolean isOneLine(TextRange textRange, Document document) {
        return document.getLineNumber(textRange.getStartOffset()) == document.getLineNumber(textRange.getEndOffset());
    }

    @Override
    public String getPlaceholderText(@NotNull ASTNode node) {
        ASTNode prev = node.getTreePrev();
        ASTNode next = node.getTreeNext();
        if (prev != null && next != null && prev.getElementType() == JetTokens.IDE_TEMPLATE_START
            && next.getElementType() == JetTokens.IDE_TEMPLATE_END) {
            return node.getText();
        }
        if (node.getElementType() == JetTokens.BLOCK_COMMENT) {
            return "/.../";
        }
        if (node.getElementType() == KDocTokens.KDOC) {
            return "/**...*/";
        }
        if (node.getPsi() instanceof JetImportDirective) {
            return "...";
        }
        return "{...}";
    }

    @Override
    public boolean isCollapsedByDefault(@NotNull ASTNode astNode) {
        JavaCodeFoldingSettings settings = JavaCodeFoldingSettings.getInstance();

        if (astNode.getPsi() instanceof JetImportDirective) {
            return settings.isCollapseImports();
        }

        IElementType type = astNode.getElementType();
        if (type == JetTokens.BLOCK_COMMENT || type == KDocTokens.KDOC) {
            if (isFirstElementInFile(astNode.getPsi())) {
                return settings.isCollapseFileHeader();
            }
        }

        return false;
    }

    private static boolean isFirstElementInFile(PsiElement element) {
        PsiElement parent = element.getParent();
        if (parent instanceof JetFile) {
            PsiElement firstChild = parent.getFirstChild();
            if (firstChild instanceof PsiWhiteSpace) {
                firstChild = firstChild.getNextSibling();
            }

            return element == firstChild;
        }

        return false;
    }
}
