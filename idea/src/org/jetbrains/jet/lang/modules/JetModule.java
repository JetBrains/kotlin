package org.jetbrains.jet.lang.modules;

import java.util.Collection;

/**
 * @author abreslav
 */
public interface JetModule extends NamespaceDomain {
    Collection<JetModule> getImports();
}
