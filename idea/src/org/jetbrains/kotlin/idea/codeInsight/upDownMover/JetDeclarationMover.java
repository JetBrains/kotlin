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
import org.jetbrains.kotlin.lexer.JetTokens;
import org.jetbrains.kotlin.psi.*;

import java.util.ArrayList;
import java.util.List;

public class JetDeclarationMover extends AbstractJetUpDownMover {
    public JetDeclarationMover() {
    }

    @NotNull
    private static List<PsiElement> getDeclarationAnchors(@NotNull JetDeclaration declaration) {
        final List<PsiElement> memberSuspects = new ArrayList<PsiElement>();

        JetModifierList modifierList = declaration.getModifierList();
        if (modifierList != null) memberSuspects.add(modifierList);

        if (declaration instanceof JetNamedDeclaration) {
            PsiElement nameIdentifier = ((JetNamedDeclaration) declaration).getNameIdentifier();
            if (nameIdentifier != null) memberSuspects.add(nameIdentifier);
        }

        declaration.accept(
                new JetVisitorVoid() {
                    @Override
                    public void visitAnonymousInitializer(@NotNull JetClassInitializer initializer) {
                        PsiElement brace = initializer.getOpenBraceNode();
                        if (brace != null) {
                            memberSuspects.add(brace);
                        }
                    }

                    @Override
                    public void visitObjectDeclaration(@NotNull JetObjectDeclaration declaration) {
                        if (declaration.isDefault()) {
                            memberSuspects.add(declaration.getClassKeyword());
                        }
                    }

                    @Override
                    public void visitNamedFunction(@NotNull JetNamedFunction function) {
                        PsiElement equalsToken = function.getEqualsToken();
                        if (equalsToken != null) memberSuspects.add(equalsToken);

                        JetTypeParameterList typeParameterList = function.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        JetTypeReference receiverTypeRef = function.getReceiverTypeReference();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        JetTypeReference returnTypeRef = function.getTypeReference();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }

                    @Override
                    public void visitProperty(@NotNull JetProperty property) {
                        PsiElement valOrVarNode = property.getValOrVarNode().getPsi();
                        if (valOrVarNode != null) memberSuspects.add(valOrVarNode);

                        JetTypeParameterList typeParameterList = property.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        JetTypeReference receiverTypeRef = property.getReceiverTypeReference();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        JetTypeReference returnTypeRef = property.getTypeReference();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }
                }
        );

        return memberSuspects;
    }

    private static final Class[] DECLARATION_CONTAINER_CLASSES =
            {JetClassBody.class, JetClassInitializer.class, JetFunction.class, JetPropertyAccessor.class, JetFile.class};

    private static final Class[] CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES = {JetClassBody.class, JetFile.class};

    @Nullable
    private static JetDeclaration getMovableDeclaration(@Nullable PsiElement element) {
        if (element == null) return null;

        JetDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetDeclaration.class, false);
        if (declaration instanceof JetParameter) return null;
        if (declaration instanceof JetTypeParameter) {
            return getMovableDeclaration(declaration.getParent());
        }

        return PsiTreeUtil.instanceOf(PsiTreeUtil.getParentOfType(declaration,
                                                                  DECLARATION_CONTAINER_CLASSES),
                                      CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES) ? declaration : null;
    }

    @Override
    protected boolean checkSourceElement(@NotNull PsiElement element) {
        return element instanceof JetDeclaration;
    }

    @Override
    protected LineRange getElementSourceLineRange(@NotNull PsiElement element, @NotNull Editor editor, @NotNull LineRange oldRange) {
        PsiElement first;
        PsiElement last;

        if (element instanceof JetDeclaration) {
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

        TextRange lineTextRange = new TextRange(doc.getLineStartOffset(oldRange.startLine),
                                                doc.getLineEndOffset(oldRange.endLine));
        if (element instanceof JetDeclaration) {
            for (PsiElement anchor : getDeclarationAnchors((JetDeclaration) element)) {
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
        if (sibling.getNode().getElementType() == (down ? JetTokens.RBRACE : JetTokens.LBRACE)) {
            // elements which aren't immediately placed in class body can't leave the block
            PsiElement parent = sibling.getParent();
            if (!(parent instanceof JetClassBody)) return null;

            JetClassOrObject jetClassOrObject = (JetClassOrObject) parent.getParent();
            assert jetClassOrObject != null;

            nextParent = jetClassOrObject.getParent();

            if (!down) {
                start = jetClassOrObject;
            }
        }
        // moving into code block
        // element may move only into class body
        else {
            if (sibling instanceof JetClassOrObject) {
                JetClassOrObject jetClassOrObject = (JetClassOrObject) sibling;
                JetClassBody classBody = jetClassOrObject.getBody();

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
            if (target instanceof JetClassInitializer && !(nextParent instanceof JetClassBody)) return null;

            if (target instanceof JetEnumEntry) {
                if (!(nextParent instanceof JetClassBody)) return null;

                JetClassOrObject nextClassOrObject = (JetClassOrObject) nextParent.getParent();
                assert nextClassOrObject != null;

                if (!nextClassOrObject.hasModifier(JetTokens.ENUM_KEYWORD)) return null;
            }
        }

        if (target instanceof JetPropertyAccessor && !(sibling instanceof JetPropertyAccessor)) return null;

        return start != null && end != null ? new LineRange(start, end, editor.getDocument()) : null;
    }

    private static boolean isEmptyPackageDirective(PsiElement adjustedSibling) {
        return adjustedSibling instanceof JetPackageDirective && adjustedSibling.getTextLength() == 0;
    }

    @Override
    public boolean checkAvailable(@NotNull Editor editor, @NotNull PsiFile file, @NotNull MoveInfo info, boolean down) {
        if (!super.checkAvailable(editor, file, info, down)) return false;

        LineRange oldRange = info.toMove;

        Pair<PsiElement, PsiElement> psiRange = getElementRange(editor, file, oldRange);
        if (psiRange == null) return false;

        JetDeclaration firstDecl = getMovableDeclaration(psiRange.getFirst());
        if (firstDecl == null) return false;

        JetDeclaration lastDecl = getMovableDeclaration(psiRange.getSecond());
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
