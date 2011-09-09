package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.psi.JetElement;
import org.jetbrains.jet.lang.types.NamespaceType;

/**
 * @author abreslav
 */
public class JetModuleUtil {
    public static NamespaceType getRootNamespaceType(JetElement expression) {
        // TODO: this is a stub: at least the modules' root namespaces must be indexed here
        return new NamespaceType("<namespace_root>", JetScope.EMPTY);
    }
}
