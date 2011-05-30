package org.jetbrains.jet.lang.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 * @author abreslav
 */
public class JetPsiUtil {
    @Nullable
    public static JetExpression deparenthesize(@NotNull JetExpression expression) {
        JetExpression result = expression;
        while (result instanceof JetParenthesizedExpression) {
            result = ((JetParenthesizedExpression) expression).getExpression();
        }
        return result;
    }

    @NotNull
    public static String safeName(String name) {
        return name == null ? "<no name provided>" : name;
    }

    @NotNull
    public static Set<JetElement> findRootExpressions(@NotNull Collection<JetElement> unreachableElements) {
        Set<JetElement> rootElements = new HashSet<JetElement>();
        final Set<JetElement> shadowedElements = new HashSet<JetElement>();
        JetVisitor shadowAllChildren = new JetVisitor() {
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
}
