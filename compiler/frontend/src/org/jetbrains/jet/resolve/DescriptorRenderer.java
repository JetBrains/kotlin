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
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

/**
 * @author abreslav
 */
public class DescriptorRenderer implements Renderer<DeclarationDescriptor> {

    public static final DescriptorRenderer COMPACT = new DescriptorRenderer() {
        @Override
        protected boolean shouldRenderDefinedIn() {
            return false;
        }
    };

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
        return renderType(type, false);
    }

    public String renderTypeWithShortNames(JetType type) {
        return renderType(type, true);
    }

    private String renderType(JetType type, boolean shortNamesOnly) {
        if (type == null) {
            return escape("[NULL]");
        }
        else if (JetStandardClasses.isUnit(type)) {
            return escape("Unit" + (type.isNullable() ? "?" : ""));
        }
        else if (JetStandardClasses.isTupleType(type)) {
            return escape(renderTupleType(type, shortNamesOnly));
        }
        else if (JetStandardClasses.isFunctionType(type)) {
            return escape(renderFunctionType(type, shortNamesOnly));
        }
        else {
            return escape(renderDefaultType(type, shortNamesOnly));
        }
    }

    private String renderDefaultType(JetType type, boolean shortNamesOnly) {
        StringBuilder sb = new StringBuilder();
        ClassifierDescriptor cd = type.getConstructor().getDeclarationDescriptor();

        Object typeNameObject;

        if (cd == null || cd instanceof TypeParameterDescriptor) {
            typeNameObject = type.getConstructor();
        }
        else {
            typeNameObject = shortNamesOnly ? cd.getName() : DescriptorUtils.getFQName(cd);
        }

        sb.append(typeNameObject);
        if (!type.getArguments().isEmpty()) {
            sb.append("<");
            appendTypeProjections(sb, type.getArguments(), shortNamesOnly);
            sb.append(">");
        }
        if (type.isNullable()) {
            sb.append("?");
        }
        return sb.toString();
    }

    private void appendTypes(StringBuilder result, List<JetType> types, boolean shortNamesOnly) {
        for (Iterator<JetType> iterator = types.iterator(); iterator.hasNext(); ) {
            result.append(renderType(iterator.next(), shortNamesOnly));
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
    }

    private void appendTypeProjections(StringBuilder result, List<TypeProjection> typeProjections, boolean shortNamesOnly) {
        for (Iterator<TypeProjection> iterator = typeProjections.iterator(); iterator.hasNext(); ) {
            TypeProjection typeProjection = iterator.next();
            if (typeProjection.getProjectionKind() != Variance.INVARIANT) {
                result.append(typeProjection.getProjectionKind()).append(" ");
            }
            result.append(renderType(typeProjection.getType(), shortNamesOnly));
            if (iterator.hasNext()) {
                result.append(", ");
            }
        }
    }

    protected String renderTupleType(JetType type, boolean shortNamesOnly) {
        StringBuilder sb = new StringBuilder("#(");
        appendTypes(sb, JetStandardClasses.getTupleElementTypes(type), shortNamesOnly);
        sb.append(")");

        if (type.isNullable()) {
            sb.append("?");
        }

        return sb.toString();
    }

    private String renderFunctionType(JetType type, boolean shortNamesOnly) {
        StringBuilder sb = new StringBuilder();

        JetType receiverType = JetStandardClasses.getReceiverType(type);
        if (receiverType != null) {
            sb.append(renderType(receiverType, shortNamesOnly));
            sb.append(".");
        }

        sb.append("(");
        appendTypeProjections(sb, JetStandardClasses.getParameterTypeProjectionsFromFunctionType(type), shortNamesOnly);
        sb.append(") -> ");
        sb.append(renderType(JetStandardClasses.getReturnTypeFromFunctionType(type), shortNamesOnly));

        if (type.isNullable()) {
            return "(" + sb + ")?";
        }
        return sb.toString();
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
        if (shouldRenderDefinedIn()) {
            appendDefinedIn(declarationDescriptor, stringBuilder);
        }
        return stringBuilder.toString();
    }

    protected boolean shouldRenderDefinedIn() {
        return true;
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
        if (shouldRenderDefinedIn()) {
            appendDefinedIn(classDescriptor, stringBuilder);
        }
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
            if (renderTypeParameters(descriptor.getTypeParameters(), builder)) {
                builder.append(" ");
            }

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

        private boolean renderTypeParameters(List<TypeParameterDescriptor> typeParameters, StringBuilder builder) {
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
            return false;
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
            String keyword;
            switch (descriptor.getKind()) {
                case TRAIT:
                    keyword = "trait";
                    break;
                case ENUM_CLASS:
                    keyword = "enum class";
                    break;
                case OBJECT:
                    keyword = "object";
                    break;
                default:
                    keyword = "class";
            }
            renderClassDescriptor(descriptor, builder, keyword);
            return super.visitClassDescriptor(descriptor, builder);
        }

        public void renderClassDescriptor(ClassDescriptor descriptor, StringBuilder builder, String keyword) {
            if (descriptor.getKind() != ClassKind.TRAIT && descriptor.getKind() != ClassKind.OBJECT) {
                renderModality(descriptor.getModality(), builder);
            }
            builder.append(renderKeyword(keyword));
            if (descriptor.getKind() != ClassKind.OBJECT) {
                builder.append(" ");
                renderName(descriptor, builder);
                renderTypeParameters(descriptor.getTypeConstructor().getParameters(), builder);
            }
            if (!descriptor.equals(JetStandardClasses.getNothing())) {
                Collection<? extends JetType> supertypes = descriptor.getTypeConstructor().getSupertypes();
                if (supertypes.isEmpty() || supertypes.size() == 1 && JetStandardClasses.isAny(supertypes.iterator().next())) {
                } else {
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
