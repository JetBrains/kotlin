package org.jetbrains.jet.resolve;

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorUtil {

    private static final DeclarationDescriptorVisitor<Void, StringBuilder> rootVisitor = new RenderDeclarationDescriptorVisitor();
    private static final DeclarationDescriptorVisitor<Void, StringBuilder> subVisitor = new RenderDeclarationDescriptorVisitor() {
        @Override
        protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
            stringBuilder.append(descriptor.getName());
        }
    };

    public static String renderPresentableText(DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor == null) return "<null>";
        StringBuilder stringBuilder = new StringBuilder();
        declarationDescriptor.accept(rootVisitor, stringBuilder);
        return stringBuilder.toString();
    }

    public static String getFQName(DeclarationDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container != null && !(container instanceof ModuleDescriptor)) {
            String baseName = getFQName(container);
            if (!baseName.isEmpty()) return baseName + "." + descriptor.getName();
        }

        return descriptor.getName();
    }

    private DescriptorUtil() {}

    private static class RenderDeclarationDescriptorVisitor extends DeclarationDescriptorVisitor<Void, StringBuilder> {
        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            builder.append("value-parameter ");
            return super.visitValueParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            JetType outType = descriptor.getOutType();
            JetType inType = descriptor.getInType();
            String typeString = "<no type>";
            if (inType != null && outType != null) {
                builder.append("var ");
                if (inType.equals(outType)) {
                    typeString = outType.toString();
                }
                else {
                    typeString = "<in " + inType + " out " + outType + ">";
                }
            }
            else if (outType != null) {
                builder.append("val ");
                typeString = outType.toString();
            }
            else if (inType != null) {
                builder.append("<write-only> ");
                typeString = inType.toString();
            }
            renderName(descriptor, builder);
            builder.append(" : ").append(typeString);
            return super.visitVariableDescriptor(descriptor, builder);
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
            builder.append("fun ");
            renderName(descriptor, builder);
            List<TypeParameterDescriptor> typeParameters = descriptor.getTypeParameters();
            renderTypeParameters(typeParameters, builder);
            builder.append("(");
            for (Iterator<ValueParameterDescriptor> iterator = descriptor.getUnsubstitutedValueParameters().iterator(); iterator.hasNext(); ) {
                ValueParameterDescriptor parameterDescriptor = iterator.next();
                parameterDescriptor.accept(subVisitor, builder);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(") : ").append(descriptor.getUnsubstitutedReturnType());
            return super.visitFunctionDescriptor(descriptor, builder);
        }

        private void renderTypeParameters(List<TypeParameterDescriptor> typeParameters, StringBuilder builder) {
            if (!typeParameters.isEmpty()) {
                builder.append("<");
                for (Iterator<TypeParameterDescriptor> iterator = typeParameters.iterator(); iterator.hasNext(); ) {
                    TypeParameterDescriptor typeParameterDescriptor = iterator.next();
                    typeParameterDescriptor.accept(subVisitor, builder);
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
                builder.append(">");
            }
        }

        @Override
        public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder) {
            builder.append("<");
            renderTypeParameter(descriptor, builder);
            builder.append(">");
            return super.visitTypeParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitNamespaceDescriptor(NamespaceDescriptor namespaceDescriptor, StringBuilder builder) {
            builder.append("namespace ");
            renderName(namespaceDescriptor, builder);
            return super.visitNamespaceDescriptor(namespaceDescriptor, builder);
        }

        @Override
        public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder) {
            builder.append("class ");
            renderName(descriptor, builder);
            renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
            Collection<? extends JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
            if (!supertypes.isEmpty()) {
                builder.append(" : ");
                for (Iterator<? extends JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                    JetType supertype = iterator.next();
                    builder.append(supertype);
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
            return super.visitClassDescriptor(descriptor, builder);
        }

        protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration != null) {
                renderName(containingDeclaration, stringBuilder);
                stringBuilder.append("::");
            }
            stringBuilder.append(descriptor.getName());
        }

        private void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder) {
            renderName(descriptor, builder);
            if (!descriptor.getUpperBounds().isEmpty()) {
                JetType bound = descriptor.getUpperBounds().iterator().next();
                if (bound != JetStandardClasses.getAnyType()) {
                    builder.append(" : ").append(bound);
                    if (descriptor.getUpperBounds().size() > 1) {
                        builder.append(" (...)");
                    }
                }
            }
        }
    }
}
