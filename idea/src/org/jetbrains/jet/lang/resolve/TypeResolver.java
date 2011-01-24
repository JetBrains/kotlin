package org.jetbrains.jet.lang.resolve;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.modules.MemberDomain;
import org.jetbrains.jet.lang.psi.JetReferenceExpression;
import org.jetbrains.jet.lang.psi.JetUserType;
import org.jetbrains.jet.lang.types.ClassDescriptor;
import org.jetbrains.jet.lang.types.NamespaceDescriptor;

/**
 * @author abreslav
 */
public class TypeResolver {

    public static final TypeResolver INSTANCE = new TypeResolver();

    private TypeResolver() {}

    @Nullable
    public ClassDescriptor resolveClass(JetScope scope, JetUserType userType) {
        return resolveClass(scope, userType.getReferenceExpression());
    }

    @Nullable
    private ClassDescriptor resolveClass(JetScope scope, JetReferenceExpression expression) {
        if (expression.isAbsoluteInRootNamespace()) {
            return resolveClass(JetModuleUtil.getRootNamespaceScope(expression), expression);
        }

        JetReferenceExpression qualifier = expression.getQualifier();
        if (qualifier != null) {
            // TODO: this is slow. The faster way would be to start with the first item in the quilified name
            // TODO: priorities: class of namespace first?
            MemberDomain domain = resolveClass(scope, qualifier);
            if (domain == null) {
                domain = resolveNamespace(scope, qualifier);
            }

            if (domain != null) {
                return domain.getClass(expression.getReferencedName());
            }
            return null;
        }

        assert qualifier == null;

        return scope.getClass(expression.getReferencedName());
    }

    @Nullable
    private NamespaceDescriptor resolveNamespace(JetScope scope, JetReferenceExpression expression) {
        if (expression.isAbsoluteInRootNamespace()) {
            return resolveNamespace(JetModuleUtil.getRootNamespaceScope(expression), expression);
        }

        JetReferenceExpression qualifier = expression.getQualifier();
        if (qualifier != null) {
            NamespaceDescriptor domain = resolveNamespace(scope, qualifier);
            if (domain == null) {
                return null;
            }
            return domain.getNamespace(expression.getReferencedName());
        }

        assert qualifier == null;

        return scope.getNamespace(expression.getReferencedName());
    }

}
