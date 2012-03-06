/*
 * Copyright 2010-2012 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.jet.resolve;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.diagnostics.Renderer;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetStandardClasses;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorRenderer implements Renderer<DeclarationDescriptor> {

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

    private DescriptorRenderer() {
    }

    protected String renderKeyword(String keyword) {
        return keyword;
    }

    public String renderType(JetType type) {
        if (type == null) {
            return escape("[NULL]");
        }
        else if (JetStandardClasses.isUnit(type)) {
            return escape("Unit" + (type.isNullable() ? "?" : ""));
        }
        else if (JetStandardClasses.isTupleType(type)) {
            return escape(renderTupleType(type));
        }
        else if (JetStandardClasses.isReceiverFunctionType(type)) {
            return escape(renderReceiverFunctionType(type));
        }
        else if (JetStandardClasses.isFunctionType(type)) {
            return escape(renderFunctionType(type, false));
        }
        else {
            return escape(type.toString());
        }
    }

    protected String renderTupleType(JetType type) {
        StringBuilder sb = new StringBuilder("#(");
        for (Iterator<TypeProjection> iterator = type.getArguments().iterator(); iterator.hasNext(); ) {
            TypeProjection argument = iterator.next();
            sb.append(renderType(argument.getType()));
            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }
        sb.append(")");

        if (type.isNullable()) {
            sb.append("?");
        }

        return sb.toString();
    }

    protected String renderFunctionType(JetType type, boolean receiver) {
        StringBuilder sb = new StringBuilder("(");
        List<TypeProjection> arguments = type.getArguments();
        for (int idx = receiver ? 1 : 0; idx < arguments.size(); idx++) {
            if (idx + 1 == arguments.size()) {
                sb.append(")->");
            }
            TypeProjection argument = arguments.get(idx);
            sb.append(renderType(argument.getType()));
            if (idx + 2 < arguments.size()) {
                sb.append(", ");
            }
        }

        return receiver ? sb.toString() : appendNullability(type, sb);
    }

    protected String renderReceiverFunctionType(JetType type) {
        StringBuilder sb = new StringBuilder();

        sb.append(renderType(type.getArguments().get(0).getType())).append(".").append(renderFunctionType(type, true));

        return appendNullability(type, sb);
    }

    private String appendNullability(JetType type, StringBuilder sb) {
        String rendered = sb.toString();
        if (type.isNullable()) {
            rendered = "(" + rendered + ")?";
        }
        return rendered;
    }

    protected String escape(String s) {
        return s;
    }

    private String lt() {
        return escape("<");
    }

    @NotNull
    @Override
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
            String typeString = renderPropertyPrefixAndComputeTypeString(
                    builder,
                    descriptor.isVar(),
                    Collections.<TypeParameterDescriptor>emptyList(),
                    ReceiverDescriptor.NO_RECEIVER,
                    descriptor.getType());
            renderName(descriptor, builder);
            builder.append(" : ").append(escape(typeString));
            return super.visitVariableDescriptor(descriptor, builder);
        }

        private String renderPropertyPrefixAndComputeTypeString(
                @NotNull StringBuilder builder,
                boolean isVar,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull ReceiverDescriptor receiver,
                @Nullable JetType outType) {
            String typeString = lt() + "no type>";
            if (outType != null) {
                builder.append(renderKeyword(isVar ? "var" : "val")).append(" ");
                typeString = renderType(outType);
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
                    builder,
                    descriptor.isVar(),
                    descriptor.getTypeParameters(),
                    descriptor.getReceiverParameter(),
                    descriptor.getType());
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
            builder.append(" : ").append(escape(renderType(descriptor.getReturnType())));
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
            builder.append(renderKeyword(JetTokens.PACKAGE_KEYWORD.getValue())).append(" ");
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
