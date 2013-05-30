package org.jetbrains.jet.plugin.codeInsight.upDownMover;

import com.intellij.codeInsight.editorActions.moveUpDown.LineRange;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.util.Pair;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.*;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lexer.JetTokens;

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
                    public void visitAnonymousInitializer(JetClassInitializer initializer) {
                        memberSuspects.add(initializer.getOpenBraceNode());
                    }

                    @Override
                    public void visitClassObject(JetClassObject classObject) {
                        PsiElement classKeyword = classObject.getClassKeywordNode();
                        if (classKeyword != null) memberSuspects.add(classKeyword);
                    }

                    @Override
                    public void visitNamedFunction(JetNamedFunction function) {
                        PsiElement equalsToken = function.getEqualsToken();
                        if (equalsToken != null) memberSuspects.add(equalsToken);

                        JetParameterList parameterList = function.getValueParameterList();
                        if (parameterList != null) memberSuspects.add(parameterList);

                        JetTypeParameterList typeParameterList = function.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        JetTypeReference receiverTypeRef = function.getReceiverTypeRef();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        JetTypeReference returnTypeRef = function.getReturnTypeRef();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }

                    @Override
                    public void visitProperty(JetProperty property) {
                        PsiElement valOrVarNode = property.getValOrVarNode().getPsi();
                        if (valOrVarNode != null) memberSuspects.add(valOrVarNode);

                        JetTypeParameterList typeParameterList = property.getTypeParameterList();
                        if (typeParameterList != null) memberSuspects.add(typeParameterList);

                        JetTypeReference receiverTypeRef = property.getReceiverTypeRef();
                        if (receiverTypeRef != null) memberSuspects.add(receiverTypeRef);

                        JetTypeReference returnTypeRef = property.getTypeRef();
                        if (returnTypeRef != null) memberSuspects.add(returnTypeRef);
                    }
                }
        );

        return memberSuspects;
    }

    @Nullable
    private static LineRange adjustDeclarationRange(
            @NotNull JetDeclaration declaration,
            @NotNull Editor editor,
            @NotNull LineRange lineRange
    ) {
        Document doc = editor.getDocument();
        TextRange textRange = declaration.getTextRange();
        if (doc.getTextLength() < textRange.getEndOffset()) return null;

        int startLine = editor.offsetToLogicalPosition(textRange.getStartOffset()).line;
        int endLine = editor.offsetToLogicalPosition(textRange.getEndOffset()).line + 1;

        if (startLine == lineRange.startLine || startLine == lineRange.endLine
            || endLine == lineRange.startLine || endLine == lineRange.endLine) {
            return new LineRange(startLine, endLine);
        }

        TextRange lineTextRange = new TextRange(doc.getLineStartOffset(lineRange.startLine),
                                                doc.getLineEndOffset(lineRange.endLine));
        for (PsiElement anchor : getDeclarationAnchors(declaration)) {
            TextRange suspectTextRange = anchor.getTextRange();
            if (suspectTextRange != null && lineTextRange.intersects(suspectTextRange)) return new LineRange(startLine, endLine);
        }

        return null;
    }

    private static final Class[] DECLARATION_CONTAINER_CLASSES =
            {JetClassBody.class, JetClassInitializer.class, JetFunction.class, JetPropertyAccessor.class, JetFile.class};

    private static final Class[] CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES = {JetClassBody.class, JetFile.class};

    @Nullable
    private static JetDeclaration getMovableDeclaration(@Nullable PsiElement element) {
        if (element == null) return null;

        JetDeclaration declaration = PsiTreeUtil.getParentOfType(element, JetDeclaration.class, false);

        return PsiTreeUtil.instanceOf(PsiTreeUtil.getParentOfType(declaration,
                                                                  DECLARATION_CONTAINER_CLASSES),
                                      CLASSBODYLIKE_DECLARATION_CONTAINER_CLASSES) ? declaration : null;
    }

    @Nullable
    private static LineRange getSourceRange(
            @NotNull JetDeclaration firstDecl,
            @NotNull JetDeclaration lastDecl,
            @NotNull Editor editor,
            @NotNull LineRange oldRange
    ) {
        if (firstDecl == lastDecl) {
            LineRange range = adjustDeclarationRange(firstDecl, editor, oldRange);

            if (range != null) {
                range.firstElement = range.lastElement = firstDecl;
            }

            return range;
        }

        PsiElement parent = PsiTreeUtil.findCommonParent(firstDecl, lastDecl);
        if (parent == null) return null;

        Pair<PsiElement, PsiElement> combinedRange = getElementRange(parent, firstDecl, lastDecl);

        if (combinedRange == null
            || !(combinedRange.first instanceof JetDeclaration)
            || !(combinedRange.second instanceof JetDeclaration)) {
            return null;
        }

        LineRange lineRange1 = adjustDeclarationRange((JetDeclaration) combinedRange.getFirst(), editor, oldRange);
        if (lineRange1 == null) return null;

        LineRange lineRange2 = adjustDeclarationRange((JetDeclaration) combinedRange.getSecond(), editor, oldRange);
        if (lineRange2 == null) return null;

        LineRange range = new LineRange(lineRange1.startLine, lineRange2.endLine);
        range.firstElement = combinedRange.getFirst();
        range.lastElement = combinedRange.getSecond();

        return range;
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
        else if (sibling instanceof JetClassOrObject) {
            JetClassOrObject jetClassOrObject = (JetClassOrObject) sibling;
            JetClassBody classBody = jetClassOrObject.getBody();

            // confined elements can't leave their block
            if (classBody != null) {
                nextParent = classBody;
                start = down ? jetClassOrObject : classBody.getRBrace();
                end = down ? classBody.getLBrace() : classBody.getRBrace();
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

        PsiElement sibling = firstNonWhiteSibling(sourceRange, down);

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
