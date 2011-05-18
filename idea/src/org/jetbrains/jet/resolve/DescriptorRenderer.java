package org.jetbrains.jet.resolve;

import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.types.*;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorRenderer {

    public static String getFQName(DeclarationDescriptor descriptor) {
        DeclarationDescriptor container = descriptor.getContainingDeclaration();
        if (container != null && !(container instanceof ModuleDescriptor)) {
            String baseName = getFQName(container);
            if (!baseName.isEmpty()) return baseName + "." + descriptor.getName();
        }

        return descriptor.getName();
    }

    public static final DescriptorRenderer TEXT = new DescriptorRenderer();
    public static final DescriptorRenderer HTML = new DescriptorRenderer() {

        @Override
        protected String escape(String s) {
            return s.replaceAll("<", "&lt;");
        }

        @Override
        public String renderKeyword(String keyword) {
            return "<b>" + keyword + "</b>";
        }
    };


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final DeclarationDescriptorVisitor<Void, StringBuilder> rootVisitor = new RenderDeclarationDescriptorVisitor();

    private final DeclarationDescriptorVisitor<Void, StringBuilder> subVisitor = new RenderDeclarationDescriptorVisitor() {
        @Override
        protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
            stringBuilder.append(descriptor.getName());
        }

        @Override
        public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder) {
            renderTypeParameter(descriptor, builder);
            return null;
        }

        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            return super.visitVariableDescriptor(descriptor, builder);
        }
    };

    private DescriptorRenderer() {}

    protected String renderKeyword(String keyword) {
        return keyword;
    }

    protected String escape(String s) {
        return s;
    }

    private String lt() {
        return escape("<");
    }

    public String render(DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor == null) return lt() + "null>";
        StringBuilder stringBuilder = new StringBuilder();
        declarationDescriptor.accept(rootVisitor, stringBuilder);
        return stringBuilder.toString();
    }

    private class RenderDeclarationDescriptorVisitor extends DeclarationDescriptorVisitor<Void, StringBuilder> {
        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            builder.append(renderKeyword("value-parameter")).append(" ");
            return super.visitValueParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            JetType outType = descriptor.getOutType();
            JetType inType = descriptor.getInType();
            String typeString = lt() + "no type>";
            if (inType != null && outType != null) {
                builder.append(renderKeyword("var")).append(" ");
                if (inType.equals(outType)) {
                    typeString = outType.toString();
                }
                else {
                    typeString = "<" + renderKeyword("in") + ": " + inType + " " + renderKeyword("out") + ": " + outType + ">";
                }
            }
            else if (outType != null) {
                builder.append(renderKeyword("val")).append(" ");
                typeString = outType.toString();
            }
            else if (inType != null) {
                builder.append(lt()).append("write-only> ");
                typeString = inType.toString();
            }
            renderName(descriptor, builder);
            builder.append(" : ").append(escape(typeString));
            return super.visitVariableDescriptor(descriptor, builder);
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
            builder.append(renderKeyword("fun")).append(" ");
            List<TypeParameterDescriptor> typeParameters = descriptor.getTypeParameters();
            renderTypeParameters(typeParameters, builder);
            if (!typeParameters.isEmpty()) {
                builder.append(" ");
            }

            JetType receiverType = descriptor.getReceiverType();
            if (receiverType != null) {
                builder.append(escape(receiverType.toString())).append(".");
            }

            renderName(descriptor, builder);
            builder.append("(");
            for (Iterator<ValueParameterDescriptor> iterator = descriptor.getUnsubstitutedValueParameters().iterator(); iterator.hasNext(); ) {
                ValueParameterDescriptor parameterDescriptor = iterator.next();
                parameterDescriptor.accept(subVisitor, builder);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(") : ").append(escape(descriptor.getUnsubstitutedReturnType().toString()));
            return super.visitFunctionDescriptor(descriptor, builder);
        }

        private void renderTypeParameters(List<TypeParameterDescriptor> typeParameters, StringBuilder builder) {
            if (!typeParameters.isEmpty()) {
                builder.append(lt());
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
            builder.append(lt());
            renderTypeParameter(descriptor, builder);
            builder.append(">");
            return super.visitTypeParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitNamespaceDescriptor(NamespaceDescriptor namespaceDescriptor, StringBuilder builder) {
            builder.append(renderKeyword("namespace")).append(" ");
            renderName(namespaceDescriptor, builder);
            return super.visitNamespaceDescriptor(namespaceDescriptor, builder);
        }

        @Override
        public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder) {
            builder.append(renderKeyword("class")).append(" ");
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
            stringBuilder.append(escape(descriptor.getName()));
        }

        protected void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder) {
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
