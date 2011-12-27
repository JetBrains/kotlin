package org.jetbrains.jet.lang.psi;

import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetPsiUtil {

    public static final String NO_NAME_PROVIDED = "<no name provided>";

    @Nullable
    public static JetExpression deparenthesize(@NotNull JetExpression expression) {
        if (expression instanceof JetBinaryExpressionWithTypeRHS) {
            JetSimpleNameExpression operationSign = ((JetBinaryExpressionWithTypeRHS) expression).getOperationSign();
            if (JetTokens.COLON.equals(operationSign.getReferencedNameElementType())) {
                expression = ((JetBinaryExpressionWithTypeRHS) expression).getLeft();
            }
        }
        else if (expression instanceof JetPrefixExpression) {
            if (JetTokens.LABELS.contains(((JetPrefixExpression) expression).getOperationReference().getReferencedNameElementType())) {
                JetExpression baseExpression = ((JetPrefixExpression) expression).getBaseExpression();
                if (baseExpression != null) {
                    expression = baseExpression;
                }
            }
        }
        if (expression instanceof JetParenthesizedExpression) {
            JetExpression innerExpression = ((JetParenthesizedExpression) expression).getExpression();
            return innerExpression != null ? deparenthesize(innerExpression) : null;
        }
        return expression;
    }

    @NotNull
    public static String safeName(String name) {
        return name == null ? NO_NAME_PROVIDED : name;
    }

    @NotNull
    public static Set<JetElement> findRootExpressions(@NotNull Collection<JetElement> unreachableElements) {
        Set<JetElement> rootElements = new HashSet<JetElement>();
        final Set<JetElement> shadowedElements = new HashSet<JetElement>();
        JetVisitorVoid shadowAllChildren = new JetVisitorVoid() {
            @Override
            public void visitJetElement(JetElement element) {
                if (shadowedElements.add(element)) {
                    element.acceptChildren(this);
                }
            }
        };

        for (JetElement element : unreachableElements) {
            if (shadowedElements.contains(element)) continue;
            element.acceptChildren(shadowAllChildren);

            rootElements.removeAll(shadowedElements);
            rootElements.add(element);
        }
        return rootElements;
    }

    @Nullable
    public static JetNamedFunction getSurroundingFunction(@Nullable PsiElement element) {
        while (element != null) {
            if (element instanceof JetNamedFunction) return (JetNamedFunction) element;
            if (element instanceof JetClassOrObject || element instanceof JetNamespace) return null;
            element = element.getParent();
        }
        return null;
    }

    @NotNull
    public static String unquoteIdentifier(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("`") && quoted.endsWith("`") && quoted.length() >= 2) {
            return quoted.substring(1, quoted.length() - 1);
        } else {
            return quoted;
        }
    }
    
    @NotNull
    public static String unquoteIdentifierOrFieldReference(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("$")) {
            return "$" + unquoteIdentifier(quoted.substring(1));
        } else {
            return unquoteIdentifier(quoted);
        }
    }

    public static String getFQName(JetNamespace jetNamespace) {
        JetNamespace parent = PsiTreeUtil.getParentOfType(jetNamespace, JetNamespace.class);
        if (parent != null) {
            String parentFQName = getFQName(parent);
            if (parentFQName.length() > 0) {
                return parentFQName + "." + getFQName(jetNamespace.getHeader());
            }
        }
        return getFQName(jetNamespace.getHeader()); // TODO: Must include module root namespace
    }

    private static String getFQName(JetNamespaceHeader header) {
        StringBuilder builder = new StringBuilder();
        for (JetSimpleNameExpression nameExpression : header.getParentNamespaceNames()) {
            builder.append(nameExpression.getReferencedName());
            builder.append(".");
        }
//        PsiElement nameIdentifier = header.getNameIdentifier();
        builder.append(header.getName());
        return builder.toString();
    }

    public static String getFQName(JetClass jetClass) {
        JetNamedDeclaration parent = PsiTreeUtil.getParentOfType(jetClass, JetNamespace.class, JetClass.class);
        if (parent instanceof JetNamespace) {
            return makeFQName(getFQName(((JetNamespace) parent)), jetClass);
        }
        if (parent instanceof JetClass) {
            return makeFQName(getFQName(((JetClass) parent)), jetClass);
        }
        return jetClass.getName();
    }

    private static String makeFQName(String prefix, JetClass jetClass) {
        return (prefix.length() == 0 ? "" : prefix + ".") + jetClass.getName();
    }
}
