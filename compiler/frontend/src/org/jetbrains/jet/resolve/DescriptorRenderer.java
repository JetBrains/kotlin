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
import org.jetbrains.jet.lang.diagnostics.rendering.Renderer;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.FqName;
import org.jetbrains.jet.lang.resolve.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.scopes.receivers.ReceiverDescriptor;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.Variance;
import org.jetbrains.jet.lang.types.lang.JetStandardClasses;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.*;

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

    public static final DescriptorRenderer DEBUG_TEXT = new DescriptorRenderer() {
        @Override
        protected boolean hasDefaultValue(ValueParameterDescriptor descriptor) {
            // hasDefaultValue() has effects
            return descriptor.declaresDefaultValue();
        }
    };

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
            renderTypeParameter(descriptor, builder, false);
            return null;
        }

        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            super.visitVariableDescriptor(descriptor, builder, true);
            if (hasDefaultValue(descriptor)) {
                builder.append(" = ...");
            }
            return null;
        }
    };

    private DescriptorRenderer() {
    }

    protected boolean hasDefaultValue(ValueParameterDescriptor descriptor) {
        return descriptor.hasDefaultValue();
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

    private static List<String> getOuterClassesNames(ClassDescriptor cd) {
        ArrayList<String> result = new ArrayList<String>();
        while (cd.getContainingDeclaration() instanceof ClassifierDescriptor) {
            result.add(cd.getName());
            cd = (ClassDescriptor)cd.getContainingDeclaration();
        }
        return result;
    }

    private String renderDefaultType(JetType type, boolean shortNamesOnly) {
        StringBuilder sb = new StringBuilder();
        ClassifierDescriptor cd = type.getConstructor().getDeclarationDescriptor();

        Object typeNameObject;

        if (cd == null || cd instanceof TypeParameterDescriptor) {
            typeNameObject = type.getConstructor();
        }
        else {
            if (shortNamesOnly) {
                // for nested classes qualified name should be used
                typeNameObject = cd.getName();
                DeclarationDescriptor parent = cd.getContainingDeclaration();
                while (parent instanceof ClassDescriptor) {
                    typeNameObject = parent.getName() + "." + typeNameObject;
                    parent = parent.getContainingDeclaration();
                }
            }
            else {
                typeNameObject = DescriptorUtils.getFQName(cd);
            }
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
    public String render(@NotNull DeclarationDescriptor declarationDescriptor) {
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
            FqNameUnsafe fqName = DescriptorUtils.getFQName(containingDeclaration);
            stringBuilder.append(FqName.ROOT.toUnsafe().equals(fqName) ? "root package" : escape(fqName.getFqName()));
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

    private class RenderDeclarationDescriptorVisitor extends DeclarationDescriptorVisitor<Void, StringBuilder> {

        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            builder.append(renderKeyword("value-parameter")).append(" ");
            return super.visitValueParameterDescriptor(descriptor, builder);
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            return visitVariableDescriptor(descriptor, builder, false);
        }

        protected Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder, boolean skipValVar) {
            JetType type = descriptor.getType();
            if (descriptor instanceof ValueParameterDescriptor) {
                JetType varargElementType = ((ValueParameterDescriptor)descriptor).getVarargElementType();
                if (varargElementType != null) {
                    builder.append(renderKeyword("vararg")).append(" ");
                    type = varargElementType;
                }
            }
            String typeString = renderPropertyPrefixAndComputeTypeString(
                    builder,
                    skipValVar ? null : descriptor.isVar(),
                    Collections.<TypeParameterDescriptor>emptyList(),
                    ReceiverDescriptor.NO_RECEIVER,
                    type);
            renderName(descriptor, builder);
            builder.append(" : ").append(escape(typeString));
            return null;
        }

        private String renderPropertyPrefixAndComputeTypeString(
                @NotNull StringBuilder builder,
                @Nullable Boolean isVar,
                @NotNull List<TypeParameterDescriptor> typeParameters,
                @NotNull ReceiverDescriptor receiver,
                @Nullable JetType outType) {
            String typeString = lt() + "no type>";
            if (outType != null) {
                if (isVar != null) {
                    builder.append(renderKeyword(isVar ? "var" : "val")).append(" ");
                }
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
            renderVisibility(descriptor.getVisibility(), builder);
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

        private void renderVisibility(Visibility visibility, StringBuilder builder) {
            if ("package".equals(visibility.toString())) {
                builder.append("public/*package*/ ");
            } else {
                builder.append(renderKeyword(visibility.toString())).append(" ");
            }
        }

        private void renderModality(Modality modality, StringBuilder builder) {
            String keyword = "";
            switch (modality) {
                case FINAL:
                    keyword = "final";
                    break;
                case OPEN:
                    keyword = "open";
                    break;
                case ABSTRACT:
                    keyword = "abstract";
                    break;
            }
            builder.append(renderKeyword(keyword)).append(" ");
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
            renderVisibility(descriptor.getVisibility(), builder);
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
            renderWhereSuffix(descriptor, builder);
            return null;
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

        private void renderWhereSuffix(@NotNull CallableMemberDescriptor callable, @NotNull StringBuilder builder) {
            boolean first = true;
            for (TypeParameterDescriptor typeParameter : callable.getTypeParameters()) {
                if (typeParameter.getUpperBounds().size() > 1) {
                    for (JetType upperBound : typeParameter.getUpperBounds()) {
                        if (first) {
                            builder.append(" ");
                            builder.append(renderKeyword("where"));
                            builder.append(" ");
                        }
                        else {
                            builder.append(", ");
                        }
                        builder.append(typeParameter.getName());
                        builder.append(" : ");
                        builder.append(escape(renderType(upperBound)));
                        first = false;
                    }
                }
            }
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
                return true;
            }
            return false;
        }

        @Override
        public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder) {
            builder.append(lt());
            renderTypeParameter(descriptor, builder, true);
            builder.append(">");
            return null;
        }

        @Override
        public Void visitNamespaceDescriptor(NamespaceDescriptor namespaceDescriptor, StringBuilder builder) {
            builder.append(renderKeyword(JetTokens.PACKAGE_KEYWORD.getValue())).append(" ");
            renderName(namespaceDescriptor, builder);
            return null;
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
                case ANNOTATION_CLASS:
                    keyword = "annotation class";
                    break;
                default:
                    keyword = "class";
            }
            renderClassDescriptor(descriptor, builder, keyword);
            return null;
        }

        private boolean isClassObjectDescriptor(ClassDescriptor descriptor) {
            if (descriptor.getKind() == ClassKind.OBJECT) {
                DeclarationDescriptor containing = descriptor.getContainingDeclaration();
                if (containing instanceof ClassDescriptor) {
                    return ((ClassDescriptor)containing).getClassObjectDescriptor() == descriptor;
                }
            }
            return false;
        }

        public void renderClassDescriptor(ClassDescriptor descriptor, StringBuilder builder, String keyword) {
            if (!isClassObjectDescriptor(descriptor)) {
                renderVisibility(descriptor.getVisibility(), builder);
            }
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
                }
                else {
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

        protected void renderTypeParameter(TypeParameterDescriptor descriptor, StringBuilder builder, boolean topLevel) {
            if (descriptor.isReified()) {
                String variance = descriptor.getVariance().toString();
                if (!variance.isEmpty()) {
                    builder.append(renderKeyword(variance)).append(" ");
                }
            }
            else {
                builder.append(renderKeyword("erased")).append(" ");
            }
            renderName(descriptor, builder);
            if (descriptor.getUpperBounds().size() == 1) {
                JetType upperBound = descriptor.getUpperBounds().iterator().next();
                if (upperBound != JetStandardClasses.getDefaultBound()) {
                    builder.append(" : ").append(renderType(upperBound));
                }
            }
            else if (topLevel) {
                boolean first = true;
                for (JetType upperBound : descriptor.getUpperBounds()) {
                    if (upperBound.equals(JetStandardClasses.getDefaultBound())) {
                        continue;
                    }
                    if (first) {
                        builder.append(" : ");
                    }
                    else {
                        builder.append(" & ");
                    }
                    builder.append(renderType(upperBound));
                    first = false;
                }
            }
            else {
                // rendered with "where"
            }
        }
    }
}
