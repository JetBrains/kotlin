package org.jetbrains.jet.lang.resolve;

import org.jetbrains.jet.lang.modules.MemberDomain;
import org.jetbrains.jet.lang.modules.NamespaceDomain;
import org.jetbrains.jet.lang.types.*;

/**
 * @author abreslav
 */
public interface JetScope extends NamespaceDomain, MemberDomain {
    JetScope EMPTY = new JetScopeImpl() {};

    abstract class JetScopeImpl implements JetScope {
        @Override
        public MethodDescriptor getMethods(String name) {
            return null;
        }

        @Override
        public ClassDescriptor getClass(String name) {
            return null;
        }

        @Override
        public PropertyDescriptor getProperty(String name) {
            return null;
        }

        @Override
        public ExtensionDescriptor getExtension(String name) {
            return null;
        }

        @Override
        public NamespaceDescriptor getNamespace(String namespaceName) {
            return null;
        }
    };

}
