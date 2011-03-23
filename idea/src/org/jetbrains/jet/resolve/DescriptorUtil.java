package org.jetbrains.jet.resolve;

import org.jetbrains.jet.lang.types.*;

import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorUtil {
    public static String renderPresentableText(DeclarationDescriptor declarationDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        declarationDescriptor.accept(
                new DeclarationDescriptorVisitor<Void, StringBuilder>() {
                    @Override
                    public Void visitPropertyDescriptor(PropertyDescriptor descriptor, StringBuilder builder) {
                        JetType type = descriptor.getOutType();
                        builder.append(renderName(descriptor)).append(" : ").append(type);
                        return super.visitPropertyDescriptor(descriptor, builder); 
                    }

                    @Override
                    public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
                        builder.append("fun ").append(renderName(descriptor));
                        List<TypeParameterDescriptor> typeParameters = descriptor.getTypeParameters();
                        renderTypeParameters(typeParameters, builder);
                        builder.append("(");
                        for (Iterator<ValueParameterDescriptor> iterator = descriptor.getUnsubstitutedValueParameters().iterator(); iterator.hasNext(); ) {
                            ValueParameterDescriptor parameterDescriptor = iterator.next();
                            visitPropertyDescriptor(parameterDescriptor, builder);
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
                                renderTypeParameter(typeParameterDescriptor, builder);
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
                        builder.append("namespace ").append(renderName(namespaceDescriptor));
                        return super.visitNamespaceDescriptor(namespaceDescriptor, builder);
                    }

                    @Override
                    public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder) {
                        builder.append("class ").append(renderName(descriptor));
                        renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
                        return super.visitClassDescriptor(descriptor, builder);
                    }
                },
                stringBuilder);
        return stringBuilder.toString();
    }

    private static StringBuilder renderName(DeclarationDescriptor descriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        renderName(descriptor, stringBuilder);
        return stringBuilder;
    }

    private static void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration != null) {
            renderName(containingDeclaration, stringBuilder);
            stringBuilder.append("::");
        }
        stringBuilder.append(descriptor.getName());
    }

    private static void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder) {
        builder.append(renderName(descriptor));
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
