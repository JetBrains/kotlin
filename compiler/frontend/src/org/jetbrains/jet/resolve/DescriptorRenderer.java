package org.jetbrains.jet.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Renderer;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorRenderer implements Renderer {

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

        @Override
        public String renderMessage(String s) {
            return "<i>" + s + "</i>";
        }
    };


////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

    private final RenderDeclarationDescriptorVisitor rootVisitor = new RenderDeclarationDescriptorVisitor();

    private final DeclarationDescriptorVisitor<Void, StringBuilder> subVisitor = new RenderDeclarationDescriptorVisitor() {
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

    public String renderType(JetType type) {
        if (type == null) {
            return escape("<?>");
        } else {
            return escape(type.toString());
        }
    }

    protected String escape(String s) {
        return s;
    }

    private String lt() {
        return escape("<");
    }

    @NotNull
    @Override
    public String render(@Nullable Object object) {
        assert object instanceof DeclarationDescriptor;
        return render((DeclarationDescriptor) object);
    }

    public String render(DeclarationDescriptor declarationDescriptor) {
        if (declarationDescriptor == null) return lt() + "null>";
        StringBuilder stringBuilder = new StringBuilder();
        declarationDescriptor.accept(rootVisitor, stringBuilder);
        appendDefinedIn(declarationDescriptor, stringBuilder);
        return stringBuilder.toString();
    }

    private void appendDefinedIn(DeclarationDescriptor declarationDescriptor, StringBuilder stringBuilder) {
        stringBuilder.append(" ").append(renderMessage("defined in")).append(" ");

        final DeclarationDescriptor containingDeclaration = declarationDescriptor.getContainingDeclaration();
        if (containingDeclaration != null) {
            renderFullyQualifiedName(containingDeclaration, stringBuilder);
        }
    }

    public String renderAsObject(@NotNull ClassDescriptor classDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        rootVisitor.renderClassDescriptor(classDescriptor, stringBuilder, "object");
        appendDefinedIn(classDescriptor, stringBuilder);
        return stringBuilder.toString();
    }

    public String renderMessage(String s) {
        return s;
    }

    private void renderFullyQualifiedName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration != null) {
            renderFullyQualifiedName(containingDeclaration, stringBuilder);
            stringBuilder.append(".");
        }
        stringBuilder.append(escape(descriptor.getName()));
    }

    private class RenderDeclarationDescriptorVisitor extends DeclarationDescriptorVisitor<Void, StringBuilder> {

        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            builder.append(renderKeyword("value-parameter")).append(" ");
            if (descriptor.getVarargElementType() != null) {
                builder.append(renderKeyword("vararg")).append(" ");
            }
            return super.visitValueParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            String typeString = renderPropertyPrefixAndComputeTypeString(builder, Collections.<TypeParameterDescriptor>emptyList(), ReceiverDescriptor.NO_RECEIVER, descriptor.getOutType(), descriptor.getInType());
            renderName(descriptor, builder);
            builder.append(" : ").append(escape(typeString));
            return super.visitVariableDescriptor(descriptor, builder);
        }

        private String renderPropertyPrefixAndComputeTypeString(
                @NotNull StringBuilder builder,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull ReceiverDescriptor receiver,
                @Nullable JetType outType,
                @Nullable JetType inType) {
            String typeString = lt() + "no type>";
            if (inType != null && outType != null) {
                builder.append(renderKeyword("var")).append(" ");
                if (inType.equals(outType)) {
                    typeString = renderType(outType);
                }
                else {
                    typeString = "<" + renderKeyword("in") + ": " + renderType(inType) + " " + renderKeyword("out") + ": " + renderType(outType) + ">";
                }
            }
            else if (outType != null) {
                builder.append(renderKeyword("val")).append(" ");
                typeString = renderType(outType);
            }
            else if (inType != null) {
                builder.append(lt()).append("write-only> ");
                typeString = renderType(inType);
            }

            renderTypeParameters(typeParameters, builder);

            if (receiver.exists()) {
                builder.append(escape(renderType(receiver.getType()))).append(".");
            }

            return typeString;
        }

        @Override
        public Void visitPropertyDescriptor(PropertyDescriptor descriptor, StringBuilder builder) {
            renderModality(descriptor.getModality(), builder);
            String typeString = renderPropertyPrefixAndComputeTypeString(
                    builder, descriptor.getTypeParameters(),
                    descriptor.getReceiverParameter(),
                    descriptor.getOutType(),
                    descriptor.getInType());
            renderName(descriptor, builder);
            builder.append(" : ").append(escape(typeString));
            return null;
        }

        private void renderModality(Modality modality, StringBuilder builder) {
            switch (modality) {
                case FINAL:
                    builder.append("final");
                    break;
                case OPEN:
                    builder.append("open");
                    break;
                case ABSTRACT:
                    builder.append("abstract");
                    break;
            }
            builder.append(" ");
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
            renderModality(descriptor.getModality(), builder);
            builder.append(renderKeyword("fun")).append(" ");
            renderTypeParameters(descriptor.getTypeParameters(), builder);

            ReceiverDescriptor receiver = descriptor.getReceiverParameter();
            if (receiver.exists()) {
                builder.append(escape(renderType(receiver.getType()))).append(".");
            }

            renderName(descriptor, builder);
            renderValueParameters(descriptor, builder);
            builder.append(" : ").append(escape(renderType(descriptor.getReturnTypeSafe())));
            return super.visitFunctionDescriptor(descriptor, builder);
        }

        private void renderValueParameters(FunctionDescriptor descriptor, StringBuilder builder) {
            builder.append("(");
            for (Iterator<ValueParameterDescriptor> iterator = descriptor.getValueParameters().iterator(); iterator.hasNext(); ) {
                ValueParameterDescriptor parameterDescriptor = iterator.next();
                parameterDescriptor.accept(subVisitor, builder);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(")");
        }

        @Override
        public Void visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, StringBuilder builder) {
            builder.append(renderKeyword("ctor")).append(" ");

            ClassDescriptor classDescriptor = constructorDescriptor.getContainingDeclaration();
            builder.append(classDescriptor.getName());

            renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder);
            renderValueParameters(constructorDescriptor, builder);
            return null;
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
                builder.append("> ");
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
            String keyword = descriptor.getKind() == ClassKind.TRAIT ? "trait" : "class";
            renderClassDescriptor(descriptor, builder, keyword);
            return super.visitClassDescriptor(descriptor, builder);
        }

        public void renderClassDescriptor(ClassDescriptor descriptor, StringBuilder builder, String keyword) {
            renderModality(descriptor.getModality(), builder);
            builder.append(renderKeyword(keyword)).append(" ");
            renderName(descriptor, builder);
            renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
            if (!descriptor.equals(JetStandardClasses.getNothing())) {
                Collection<? extends JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
                if (!supertypes.isEmpty()) {
                    builder.append(" : ");
                    for (Iterator<? extends JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                        JetType supertype = iterator.next();
                        builder.append(renderType(supertype));
                        if (iterator.hasNext()) {
                            builder.append(", ");
                        }
                    }
                }
            }
        }

        protected void renderName(DeclarationDescriptor descriptor, StringBuilder stringBuilder) {
            stringBuilder.append(escape(descriptor.getName()));
        }

        protected void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder) {
            if (!descriptor.isReified()) {
                builder.append(renderKeyword("erased")).append(" ");
            }
            renderName(descriptor, builder);
            if (!descriptor.getUpperBounds().isEmpty()) {
                JetType bound = descriptor.getUpperBounds().iterator().next();
                if (bound != JetStandardClasses.getDefaultBound()) {
                    builder.append(" : ").append(renderType(bound));
                    if (descriptor.getUpperBounds().size() > 1) {
                        builder.append(" (...)");
                    }
                }
            }
        }
    }
}
