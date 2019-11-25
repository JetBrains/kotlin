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

package org.jetbrains.kotlin.idea.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiComment;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;

public class KotlinDeclarationMover extends AbstractKotlinUpDownMover {

    private boolean moveEnumConstant = false;

    private static int findNearestNonWhitespace(@NotNull CharSequence sequence, int index) {
        char ch = sequence.charAt(--index);
        while (Character.isWhitespace(ch)) {
            ch = sequence.charAt(--index);
        }
        return index;
    }

    @Override
    public void afterMove(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        super.afterMove(editor, file, info, down);
        if (moveEnumConstant) {
            Document document = editor.getDocument();
            CharSequence cs = document.getCharsSequence();
            int end1 = findNearestNonWhitespace(cs, info.range1.getEndOffset());
            char c1 = cs.charAt(end1);
            int end2 = findNearestNonWhitespace(cs, info.range2.getEndOffset());
            char c2 = cs.charAt(end2);
            if (c1 == c2 || (c1 != ',' && c2 != ',')) return;
            if (c1 == ';' || c2 == ';') {
                // Replace comma with semicolon and vice versa
                document.replaceString(end1, end1 + 1, String.valueOf(c2));
                document.replaceString(end2, end2 + 1, String.valueOf(c1));
            }
            else if (c1 == ',') {
                // Move comma from the end of range1 to the end of range2
                document.deleteString(end1, end1 + 1);
                document.insertString(end2 + 1, ",");
            }
            else {
                // Move comma from the end of range2 to the end of range1
                document.deleteString(end2, end2 + 1);
                document.insertString(end1 + 1, ",");
            }
        }
    }

    @NotNull
    private static List<PsiElement> getDeclarationAnchors(@NotNull KtDeclaration declaration) {
        final List<PsiElement> memberSuspects = new ArrayList<PsiElement>();

        KtModifierList modifierList = declaration.getModifierList();
        if (modifierList != null) memberSuspects.add(modifierList);

        if (declaration instanceof KtNamedDeclaration) {
            PsiElement nameIdentifier = ((KtNamedDeclaration) declaration).getNameIdentifier();
            if (nameIdentifier != null) memberSuspects.add(nameIdentifier);
        }

        declaration.accept(
                new KtVisitorVoid() {
                    @Override
                    public void visitClassInitializer(@NotNull KtClassInitializer initializer) {
                        PsiElement brace = initializer.getOpenBraceNode();
                        if (brace != null) {
                            memberSuspects.add(brace);
                        }
                    }

                    @Override
                    public void visitNamedFunction(@NotNull KtNamedFunction function) {
                        PsiElement equalsToken = function.getEqualsToken();
                        if (equalsToken != null) memberSuspects.add(equalsToken);

                        KtTypeParameterList typeParameterList = function.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        KtTypeReference receiverTypeRef = function.getReceiverTypeReference();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        KtTypeReference returnTypeRef = function.getTypeReference();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }

                    @Override
                    public void visitProperty(@NotNull KtProperty property) {
                        PsiElement valOrVarKeyword = property.getValOrVarKeyword();
                        if (valOrVarKeyword != null) memberSuspects.add(valOrVarKeyword);

                        KtTypeParameterList typeParameterList = property.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        KtTypeReference receiverTypeRef = property.getReceiverTypeReference();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        KtTypeReference returnTypeRef = property.getTypeReference();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }
                }
        );

        return memberSuspects;
    }

    private static final Class[] DECLARATION_CONTAINER_CLASSES =
            {KtClassBody.class, KtAnonymousInitializer.class, KtFunction.class, KtPropertyAccessor.class, KtFile.class};

    private static final Class[] CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES = {KtClassBody.class, KtFile.class};

    @Nullable
    private static KtDeclaration getMovableDeclaration(@Nullable PsiElement element) {
        if (element == null) return null;

        KtDeclaration declaration = PsiTreeUtil.getParentOfType(element, KtDeclaration.class, false);
        if (declaration instanceof KtParameter) return null;
        if (declaration instanceof KtTypeParameter) {
            return getMovableDeclaration(declaration.getParent());
        }

        return PsiTreeUtil.instanceOf(PsiTreeUtil.getParentOfType(declaration,
                                                                  DECLARATION_CONTAINER_CLASSES),
                                      CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES) ? declaration : null;
    }

    @Override
    protected boolean checkSourceElement(@NotNull PsiElement element) {
        return element instanceof KtDeclaration;
    }

    @Override
    protected LineRange getElementSourceLineRange(@NotNull PsiElement element, @NotNull Editor editor, @NotNull LineRange oldRange) {
        PsiElement first;
        PsiElement last;

        if (element instanceof KtDeclaration) {
            first = element.getFirstChild();
            last = element.getLastChild();

            if (first == null || last == null) return null;
        }
        else {
            first = last = element;
        }


        TextRange textRange1 = first.getTextRange();
        TextRange textRange2 = last.getTextRange();

        Document doc = editor.getDocument();

        if (doc.getTextLength() < textRange2.getEndOffset()) return null;

        int startLine = editor.offsetToLogicalPosition(textRange1.getStartOffset()).line;
        int endLine = editor.offsetToLogicalPosition(textRange2.getEndOffset()).line + 1;

        if (element instanceof PsiComment
            || startLine == oldRange.startLine || startLine == oldRange.endLine
            || endLine == oldRange.startLine || endLine == oldRange.endLine) {
            return new LineRange(startLine, endLine);
        }

        int lineCount = doc.getLineCount();
        if (oldRange.startLine >= lineCount || oldRange.endLine >= lineCount) {
            return null;
        }

        TextRange lineTextRange = new TextRange(doc.getLineStartOffset(oldRange.startLine),
                                                doc.getLineEndOffset(oldRange.endLine));
        if (element instanceof KtDeclaration) {
            for (PsiElement anchor : getDeclarationAnchors((KtDeclaration) element)) {
                TextRange suspectTextRange = anchor.getTextRange();
                if (suspectTextRange != null && lineTextRange.intersects(suspectTextRange)) return new LineRange(startLine, endLine);
            }
        }

        return null;
    }

    @Nullable
    private static LineRange getTargetRange(
            @NotNull Editor editor,
            @NotNull PsiElement sibling,
            boolean down,
            @NotNull PsiElement target
    ) {
        PsiElement start = sibling;
        PsiElement end = sibling;

        PsiElement nextParent = null;

        // moving out of code block
        if (sibling.getNode().getElementType() == (down ? KtTokens.RBRACE : KtTokens.LBRACE)) {
            // elements which aren't immediately placed in class body can't leave the block
            PsiElement parent = sibling.getParent();
            if (!(parent instanceof KtClassBody)) return null;
            if (target instanceof KtEnumEntry) return null;

            KtClassOrObject ktClassOrObject = (KtClassOrObject) parent.getParent();
            assert ktClassOrObject != null;

            nextParent = ktClassOrObject.getParent();

            if (!down) {
                start = ktClassOrObject;
            }
        }
        // moving into code block
        // element may move only into class body
        else {
            if (sibling instanceof KtClassOrObject) {
                KtClassOrObject ktClassOrObject = (KtClassOrObject) sibling;
                KtClassBody classBody = ktClassOrObject.getBody();

                // confined elements can't leave their block
                if (classBody != null) {
                    nextParent = classBody;
                    if (!down) {
                        start = classBody.getRBrace();
                    }
                    end = down ? classBody.getLBrace() : classBody.getRBrace();
                }
            }
        }

        if (nextParent != null) {
            if (target instanceof KtAnonymousInitializer && !(nextParent instanceof KtClassBody)) return null;

            if (target instanceof KtEnumEntry) {
                if (!(nextParent instanceof KtClassBody)) return null;

                KtClassOrObject nextClassOrObject = (KtClassOrObject) nextParent.getParent();
                assert nextClassOrObject != null;

                if (!nextClassOrObject.hasModifier(KtTokens.ENUM_KEYWORD)) return null;
            }
        }

        if (target instanceof KtPropertyAccessor && !(sibling instanceof KtPropertyAccessor)) return null;

        return start != null && end != null ? new LineRange(start, end, editor.getDocument()) : null;
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        if (!super.checkAvailable(editor, file, info, down)) return false;

        LineRange oldRange = info.toMove;

        Pair<PsiElement, PsiElement> psiRange = getElementRange(editor, file, oldRange);
        if (psiRange == null) return false;

        KtDeclaration firstDecl = getMovableDeclaration(psiRange.getFirst());
        if (firstDecl == null) return false;

        moveEnumConstant = firstDecl instanceof KtEnumEntry;

        KtDeclaration lastDecl = getMovableDeclaration(psiRange.getSecond());
        if (lastDecl == null) return false;

        //noinspection ConstantConditions
        LineRange sourceRange = getSourceRange(firstDecl, lastDecl, editor, oldRange);
        if (sourceRange == null) return false;

        PsiElement sibling = getLastNonWhiteSiblingInLine(firstNonWhiteSibling(sourceRange, down), editor, down);

        // Either reached last sibling, or jumped over multi-line whitespace
        if (sibling == null)  {
            info.toMove2 = null;
            return true;
        }

        info.toMove = sourceRange;
        info.toMove2 = getTargetRange(editor, sibling, down, sourceRange.firstElement);
        return true;
    }
}
