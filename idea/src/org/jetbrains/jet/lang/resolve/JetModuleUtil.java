package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;

/**
 * @author abreslav
 */
public class JetModuleUtil {
    public static JetScope getRootNamespaceScope(JetElement expression) {
        // TODO: this is a stub: at least the modules' root namespaces must be indexed here
        return JetScope.EMPTY;
    }
}
