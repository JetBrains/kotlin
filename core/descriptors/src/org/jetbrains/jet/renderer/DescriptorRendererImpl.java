/*
 * Copyright 2010-2013 JetBrains s.r.o.
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

package org.jetbrains.jet.renderer;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.intellij.openapi.util.text.StringUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.annotations.Annotated;
import org.jetbrains.jet.lang.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.jet.lang.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lang.resolve.name.FqNameBase;
import org.jetbrains.jet.lang.resolve.name.FqNameUnsafe;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;

import java.util.*;

import static org.jetbrains.jet.lang.types.TypeUtils.CANT_INFER_LAMBDA_PARAM_TYPE;
import static org.jetbrains.jet.lang.types.TypeUtils.CANT_INFER_TYPE_PARAMETER;

public class DescriptorRendererImpl implements DescriptorRenderer {
    private final boolean shortNames;
    private final boolean withDefinedIn;
    private final Set<DescriptorRenderer.Modifier> modifiers;
    private final boolean startFromName;
    private final boolean debugMode;
    private final boolean classWithPrimaryConstructor;
    private final boolean verbose;
    private final boolean unitReturnType;
    private final boolean normalizedVisibilities;
    private final boolean showInternalKeyword;
    @NotNull
    private final OverrideRenderingPolicy overrideRenderingPolicy;
    @NotNull
    private final ValueParametersHandler handler;
    @NotNull
    private final TextFormat textFormat;
    @NotNull
    private final Set<FqName> excludedAnnotationClasses;

    /* package */ DescriptorRendererImpl(
            boolean shortNames,
            boolean withDefinedIn,
            Set<DescriptorRenderer.Modifier> modifiers,
            boolean startFromName,
            boolean debugMode,
            boolean classWithPrimaryConstructor,
            boolean verbose,
            boolean unitReturnType,
            boolean normalizedVisibilities,
            boolean showInternalKeyword,
            @NotNull OverrideRenderingPolicy overrideRenderingPolicy,
            @NotNull ValueParametersHandler handler,
            @NotNull TextFormat textFormat,
            @NotNull Collection<FqName> excludedAnnotationClasses
    ) {
        this.shortNames = shortNames;
        this.withDefinedIn = withDefinedIn;
        this.modifiers = modifiers;
        this.startFromName = startFromName;
        this.handler = handler;
        this.classWithPrimaryConstructor = classWithPrimaryConstructor;
        this.verbose = verbose;
        this.unitReturnType = unitReturnType;
        this.normalizedVisibilities = normalizedVisibilities;
        this.showInternalKeyword = showInternalKeyword;
        this.overrideRenderingPolicy = overrideRenderingPolicy;
        this.debugMode = debugMode;
        this.textFormat = textFormat;
        this.excludedAnnotationClasses = Sets.newHashSet(excludedAnnotationClasses);
    }


    /* FORMATTING */
    @NotNull
    private String renderKeyword(@NotNull String keyword) {
        switch (textFormat) {
            case PLAIN:
                return keyword;
            case HTML:
                return "<b>" + keyword + "</b>";
        }
        throw new IllegalStateException("Unexpected textFormat: " + textFormat);
    }

    @NotNull
    private String escape(@NotNull String string) {
        switch (textFormat) {
            case PLAIN:
                return string;
            case HTML:
                return string.replaceAll("<", "&lt;").replaceAll(">", "&gt;");
        }
        throw new IllegalStateException("Unexpected textFormat: " + textFormat);
    }

    @NotNull
    private String lt() {
        return escape("<");
    }

    @NotNull
    private String arrow() {
        switch (textFormat) {
            case PLAIN:
                return escape("->");
            case HTML:
                return "&rarr;";
        }
        throw new IllegalStateException("Unexpected textFormat: " + textFormat);
    }

    @NotNull
    private String renderMessage(@NotNull String message) {
        switch (textFormat) {
            case PLAIN:
                return message;
            case HTML:
                return "<i>" + message + "</i>";
        }
        throw new IllegalStateException("Unexpected textFormat: " + textFormat);
    }


    /* NAMES RENDERING */
    @NotNull
    private String renderName(@NotNull Name identifier) {
        String asString = identifier.toString();
        return escape(KeywordStringsGenerated.KEYWORDS.contains(asString) ? '`' + asString + '`' : asString);
    }

    private void renderName(@NotNull DeclarationDescriptor descriptor, @NotNull StringBuilder builder) {
        builder.append(renderName(descriptor.getName()));
    }

    @NotNull
    private String renderFqName(@NotNull FqNameBase fqName) {
        return renderFqName(fqName.pathSegments());
    }


    @NotNull
    private String renderFqName(@NotNull List<Name> pathSegments) {
        StringBuilder buf = new StringBuilder();
        for (Name element : pathSegments) {
            if (buf.length() != 0) {
                buf.append(".");
            }
            buf.append(renderName(element));
        }
        return buf.toString();
    }

    @NotNull
    private String renderClassName(@NotNull ClassDescriptor klass) {
        if (ErrorUtils.isError(klass)) {
            return klass.getTypeConstructor().toString();
        }
        if (shortNames) {
            List<Name> qualifiedNameElements = Lists.newArrayList();

            // for nested classes qualified name should be used
            DeclarationDescriptor current = klass;
            do {
                if (((ClassDescriptor) current).getKind() != ClassKind.CLASS_OBJECT) {
                    qualifiedNameElements.add(current.getName());
                }
                current = current.getContainingDeclaration();
            }
            while (current instanceof ClassDescriptor);

            Collections.reverse(qualifiedNameElements);
            return renderFqName(qualifiedNameElements);
        }
        return renderFqName(DescriptorUtils.getFqName(klass));
    }

    /* TYPES RENDERING */
    @NotNull
    @Override
    public String renderType(@NotNull JetType type) {
        return escape(renderTypeWithoutEscape(type));
    }

    private String renderTypeWithoutEscape(@NotNull JetType type) {
        if (type == CANT_INFER_LAMBDA_PARAM_TYPE || type == CANT_INFER_TYPE_PARAMETER) {
            return "???";
        }
        if (type.isError()) {
            return type.toString();
        }
        if (KotlinBuiltIns.getInstance().isExactFunctionOrExtensionFunctionType(type)) {
            return renderFunctionType(type);
        }
        return renderDefaultType(type);
    }

    @NotNull
    private String renderDefaultType(@NotNull JetType type) {
        StringBuilder sb = new StringBuilder();

        sb.append(renderTypeName(type.getConstructor()));
        if (!type.getArguments().isEmpty()) {
            sb.append("<");
            appendTypeProjections(type.getArguments(), sb);
            sb.append(">");
        }
        if (type.isNullable()) {
            sb.append("?");
        }
        return sb.toString();
    }

    @NotNull
    private String renderTypeName(@NotNull TypeConstructor typeConstructor) {
        ClassifierDescriptor cd = typeConstructor.getDeclarationDescriptor();
        if (cd instanceof TypeParameterDescriptor) {
            return renderName(cd.getName());
        }
        else if (cd instanceof ClassDescriptor) {
            return renderClassName((ClassDescriptor) cd);
        }
        else {
            assert cd == null: "Unexpected classifier: " + cd.getClass();
            return typeConstructor.toString();
        }
    }

    private void appendTypeProjections(@NotNull List<TypeProjection> typeProjections, @NotNull StringBuilder builder) {
        for (Iterator<TypeProjection> iterator = typeProjections.iterator(); iterator.hasNext(); ) {
            TypeProjection typeProjection = iterator.next();
            if (typeProjection.getProjectionKind() != Variance.INVARIANT) {
                builder.append(typeProjection.getProjectionKind()).append(" ");
            }
            builder.append(renderType(typeProjection.getType()));
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }

    @NotNull
    private String renderFunctionType(@NotNull JetType type) {
        StringBuilder sb = new StringBuilder();

        JetType receiverType = KotlinBuiltIns.getInstance().getReceiverType(type);
        if (receiverType != null) {
            sb.append(renderType(receiverType));
            sb.append(".");
        }

        sb.append("(");
        appendTypeProjections(KotlinBuiltIns.getInstance().getParameterTypeProjectionsFromFunctionType(type), sb);
        sb.append(") " + arrow() + " ");
        sb.append(renderType(KotlinBuiltIns.getInstance().getReturnTypeFromFunctionType(type)));

        if (type.isNullable()) {
            return "(" + sb + ")?";
        }
        return sb.toString();
    }


    /* METHODS FOR ALL KINDS OF DESCRIPTORS */
    private void appendDefinedIn(@NotNull DeclarationDescriptor descriptor, @NotNull StringBuilder builder) {
        if (descriptor instanceof PackageFragmentDescriptor || descriptor instanceof PackageViewDescriptor) {
            return;
        }
        if (descriptor instanceof ModuleDescriptor) {
            builder.append(" is a module");
            return;
        }
        builder.append(" ").append(renderMessage("defined in")).append(" ");

        DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
        if (containingDeclaration != null) {
            FqNameUnsafe fqName = DescriptorUtils.getFqName(containingDeclaration);
            builder.append(FqName.ROOT.equalsTo(fqName) ? "root package" : renderFqName(fqName));
        }
    }

    private void renderAnnotations(@NotNull Annotated annotated, @NotNull StringBuilder builder) {
        if (!modifiers.contains(Modifier.ANNOTATIONS)) return;
        for (AnnotationDescriptor annotation : annotated.getAnnotations()) {
            ClassDescriptor annotationClass = (ClassDescriptor) annotation.getType().getConstructor().getDeclarationDescriptor();
            assert annotationClass != null;

            if (!excludedAnnotationClasses.contains(DescriptorUtils.getFqNameSafe(annotationClass))) {
                builder.append(renderType(annotation.getType()));
                if (verbose) {
                    builder.append("(").append(StringUtil.join(DescriptorUtils.getSortedValueArguments(annotation, this), ", ")).append(")");
                }
                builder.append(" ");
            }
        }
    }

    private void renderVisibility(@NotNull Visibility visibility, @NotNull StringBuilder builder) {
        if (!modifiers.contains(Modifier.VISIBILITY)) return;
        if (normalizedVisibilities) {
            visibility = visibility.normalize();
        }
        if (!showInternalKeyword && visibility == Visibilities.INTERNAL) return;
        builder.append(renderKeyword(visibility.toString())).append(" ");
    }

    private void renderModality(@NotNull Modality modality, @NotNull StringBuilder builder) {
        if (!modifiers.contains(Modifier.MODALITY)) return;
        String keyword = modality.name().toLowerCase();
        builder.append(renderKeyword(keyword)).append(" ");
    }

    private void renderInner(boolean isInner, @NotNull StringBuilder builder) {
        if (!modifiers.contains(Modifier.INNER)) return;
        if (isInner) {
            builder.append(renderKeyword("inner")).append(" ");
        }
    }

    private void renderModalityForCallable(@NotNull CallableMemberDescriptor callable, @NotNull StringBuilder builder) {
        if (!DescriptorUtils.isTopLevelDeclaration(callable) || callable.getModality() != Modality.FINAL) {
            if (overridesSomething(callable)
                && overrideRenderingPolicy == OverrideRenderingPolicy.RENDER_OVERRIDE
                && callable.getModality() == Modality.OPEN) {
                return;
            }
            renderModality(callable.getModality(), builder);
        }
    }

    private boolean overridesSomething(CallableMemberDescriptor callable) {
        return !callable.getOverriddenDescriptors().isEmpty();
    }

    private void renderOverride(@NotNull CallableMemberDescriptor callableMember, @NotNull StringBuilder builder) {
        if (!modifiers.contains(Modifier.OVERRIDE)) return;
        if (overridesSomething(callableMember)) {
            if (overrideRenderingPolicy != OverrideRenderingPolicy.RENDER_OPEN) {
                builder.append("override ");
                if (verbose) {
                    builder.append("/*").append(callableMember.getOverriddenDescriptors().size()).append("*/ ");
                }
            }
        }
    }

    private void renderMemberKind(CallableMemberDescriptor callableMember, StringBuilder builder) {
        if (!modifiers.contains(Modifier.MEMBER_KIND)) return;
        if (verbose && callableMember.getKind() != CallableMemberDescriptor.Kind.DECLARATION) {
            builder.append("/*").append(callableMember.getKind().name().toLowerCase()).append("*/ ");
        }
    }

    @NotNull
    @Override
    public String render(@NotNull DeclarationDescriptor declarationDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        declarationDescriptor.accept(new RenderDeclarationDescriptorVisitor(), stringBuilder);

        if (withDefinedIn) {
            appendDefinedIn(declarationDescriptor, stringBuilder);
        }
        return stringBuilder.toString();
    }


    /* TYPE PARAMETERS */
    private void renderTypeParameter(@NotNull TypeParameterDescriptor typeParameter, @NotNull StringBuilder builder, boolean topLevel) {
        if (topLevel) {
            builder.append(lt());
        }

        if (verbose) {
            builder.append("/*").append(typeParameter.getIndex()).append("*/ ");
        }

        if (typeParameter.isReified()) {
            builder.append(renderKeyword("reified")).append(" ");
        }
        String variance = typeParameter.getVariance().toString();
        if (!variance.isEmpty()) {
            builder.append(renderKeyword(variance)).append(" ");
        }
        renderName(typeParameter, builder);
        int upperBoundsCount = typeParameter.getUpperBounds().size();
        if ((upperBoundsCount > 1 && !topLevel) || upperBoundsCount == 1) {
            JetType upperBound = typeParameter.getUpperBounds().iterator().next();
            if (!KotlinBuiltIns.getInstance().getDefaultBound().equals(upperBound)) {
                builder.append(" : ").append(renderType(upperBound));
            }
        }
        else if (topLevel) {
            boolean first = true;
            for (JetType upperBound : typeParameter.getUpperBounds()) {
                if (upperBound.equals(KotlinBuiltIns.getInstance().getDefaultBound())) {
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

        if (topLevel) {
            builder.append(">");
        }
    }

    private void renderTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull StringBuilder builder,
            boolean withSpace
    ) {
        if (!typeParameters.isEmpty()) {
            builder.append(lt());
            for (Iterator<TypeParameterDescriptor> iterator = typeParameters.iterator(); iterator.hasNext(); ) {
                TypeParameterDescriptor typeParameterDescriptor = iterator.next();
                renderTypeParameter(typeParameterDescriptor, builder, false);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(">");
            if (withSpace) {
                builder.append(" ");
            }
        }
    }

    /* FUNCTIONS */
    private void renderFunction(@NotNull FunctionDescriptor function, @NotNull StringBuilder builder) {
        if (!startFromName) {
            renderAnnotations(function, builder);
            renderVisibility(function.getVisibility(), builder);
            renderModalityForCallable(function, builder);
            renderOverride(function, builder);
            renderMemberKind(function, builder);

            builder.append(renderKeyword("fun")).append(" ");
            renderTypeParameters(function.getTypeParameters(), builder, true);

            ReceiverParameterDescriptor receiver = function.getReceiverParameter();
            if (receiver != null) {
                builder.append(escape(renderType(receiver.getType()))).append(".");
            }
        }

        renderName(function, builder);
        renderValueParameters(function, builder);
        JetType returnType = function.getReturnType();
        if (unitReturnType || !KotlinBuiltIns.getInstance().isUnit(returnType)) {
            builder.append(": ").append(returnType == null ? "[NULL]" : escape(renderType(returnType)));
        }
        renderWhereSuffix(function.getTypeParameters(), builder);
    }

    private void renderConstructor(@NotNull ConstructorDescriptor constructor, @NotNull StringBuilder builder) {
        renderAnnotations(constructor, builder);
        renderVisibility(constructor.getVisibility(), builder);
        renderMemberKind(constructor, builder);

        builder.append(renderKeyword("constructor")).append(" ");

        ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
        renderName(classDescriptor, builder);

        renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder, false);
        renderValueParameters(constructor, builder);
        renderWhereSuffix(constructor.getTypeParameters(), builder);
    }

    private void renderWhereSuffix(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull StringBuilder builder) {
        List<String> upperBoundStrings = Lists.newArrayList();

        for (TypeParameterDescriptor typeParameter : typeParameters) {
            if (typeParameter.getUpperBounds().size() > 1) {
                boolean first = true;
                for (JetType upperBound : typeParameter.getUpperBounds()) {
                    // first parameter is rendered by renderTypeParameter:
                    if (!first) {
                        upperBoundStrings.add(renderName(typeParameter.getName()) + " : " + escape(renderType(upperBound)));
                    }
                    first = false;
                }
            }
        }
        if (!upperBoundStrings.isEmpty()) {
            builder.append(" ").append(renderKeyword("where")).append(" ");
            builder.append(StringUtil.join(upperBoundStrings, ", "));
        }
    }

    @NotNull
    @Override
    public String renderFunctionParameters(@NotNull FunctionDescriptor functionDescriptor) {
        StringBuilder stringBuilder = new StringBuilder();
        renderValueParameters(functionDescriptor, stringBuilder);
        return stringBuilder.toString();
    }

    private void renderValueParameters(@NotNull FunctionDescriptor function, @NotNull StringBuilder builder) {
        handler.appendBeforeValueParameters(function, builder);
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            handler.appendBeforeValueParameter(parameter, builder);
            renderValueParameter(parameter, builder, false);
            handler.appendAfterValueParameter(parameter, builder);
        }
        handler.appendAfterValueParameters(function, builder);
    }

    /* VARIABLES */
    private void renderValueParameter(@NotNull ValueParameterDescriptor valueParameter, @NotNull StringBuilder builder, boolean topLevel) {
        if (topLevel) {
            builder.append(renderKeyword("value-parameter")).append(" ");
        }

        if (verbose) {
            builder.append("/*").append(valueParameter.getIndex()).append("*/ ");
        }

        renderAnnotations(valueParameter, builder);
        renderVariable(valueParameter, builder, topLevel);
        boolean withDefaultValue = debugMode ? valueParameter.declaresDefaultValue() : valueParameter.hasDefaultValue();
        if (withDefaultValue) {
            builder.append(" = ...");
        }
    }

    private void renderValVarPrefix(@NotNull VariableDescriptor variable, @NotNull StringBuilder builder) {
        builder.append(renderKeyword(variable.isVar() ? "var" : "val")).append(" ");
    }

    private void renderVariable(@NotNull VariableDescriptor variable, @NotNull StringBuilder builder, boolean topLevel) {
        JetType realType = variable.getType();

        JetType varargElementType = variable instanceof ValueParameterDescriptor
                                    ? ((ValueParameterDescriptor) variable).getVarargElementType()
                                    : null;
        JetType typeToRender = varargElementType != null ? varargElementType : realType;

        if (varargElementType != null) {
            builder.append(renderKeyword("vararg")).append(" ");
        }
        if (topLevel && !startFromName) {
            renderValVarPrefix(variable, builder);
        }

        renderName(variable, builder);
        builder.append(": ").append(escape(renderType(typeToRender)));

        if (verbose && varargElementType != null) {
            builder.append(" /*").append(escape(renderType(realType))).append("*/");
        }
    }

    private void renderProperty(@NotNull PropertyDescriptor property, @NotNull StringBuilder builder) {
        if (!startFromName) {
            renderAnnotations(property, builder);
            renderVisibility(property.getVisibility(), builder);
            renderModalityForCallable(property, builder);
            renderOverride(property, builder);
            renderMemberKind(property, builder);

            renderValVarPrefix(property, builder);
        }

        renderTypeParameters(property.getTypeParameters(), builder, true);

        ReceiverParameterDescriptor receiver = property.getReceiverParameter();
        if (receiver != null) {
            builder.append(escape(renderType(receiver.getType()))).append(".");
        }
        renderName(property, builder);
        builder.append(": ").append(escape(renderType(property.getType())));

        renderWhereSuffix(property.getTypeParameters(), builder);
    }


    /* CLASSES */
    private void renderClass(@NotNull ClassDescriptor klass, @NotNull StringBuilder builder) {
        if (!startFromName) {
            renderAnnotations(klass, builder);
            renderVisibility(klass.getVisibility(), builder);
            if (!(klass.getKind() == ClassKind.TRAIT && klass.getModality() == Modality.ABSTRACT
                || klass.getKind().isSingleton() && klass.getModality() == Modality.FINAL)) {
                renderModality(klass.getModality(), builder);
            }
            renderInner(klass.isInner(), builder);
            builder.append(renderKeyword(getClassKindPrefix(klass)));
        }

        if (klass.getKind() != ClassKind.CLASS_OBJECT || verbose) {
            builder.append(" ");
            renderName(klass, builder);
        }

        List<TypeParameterDescriptor> typeParameters = klass.getTypeConstructor().getParameters();
        renderTypeParameters(typeParameters, builder, false);

        if (!klass.getKind().isSingleton() && classWithPrimaryConstructor) {
            ConstructorDescriptor primaryConstructor = klass.getUnsubstitutedPrimaryConstructor();
            if (primaryConstructor != null) {
                renderValueParameters(primaryConstructor, builder);
            }
        }

        if (!klass.equals(KotlinBuiltIns.getInstance().getNothing())) {
            Collection<JetType> supertypes = klass.getTypeConstructor().getSupertypes();
            if (supertypes.isEmpty() || supertypes.size() == 1 && KotlinBuiltIns.getInstance().isAnyOrNullableAny(
                    supertypes.iterator().next())) {
            }
            else {
                builder.append(" : ");
                for (Iterator<JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                    JetType supertype = iterator.next();
                    builder.append(renderType(supertype));
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
        }

        renderWhereSuffix(typeParameters, builder);
    }

    @NotNull
    private static String getClassKindPrefix(@NotNull ClassDescriptor klass) {
        switch (klass.getKind()) {
            case CLASS:
                return "class";
            case TRAIT:
                return "trait";
            case ENUM_CLASS:
                return "enum class";
            case OBJECT:
                return "object";
            case ANNOTATION_CLASS:
                return "annotation class";
            case CLASS_OBJECT:
                return "class object";
            case ENUM_ENTRY:
                return "enum entry";
            default:
                throw new IllegalStateException("unknown class kind: " + klass.getKind());
        }
    }


    /* OTHER */
    private void renderModuleOrScript(@NotNull DeclarationDescriptor moduleOrScript, @NotNull StringBuilder builder) {
        renderName(moduleOrScript, builder);
    }

    private void renderPackageView(@NotNull PackageViewDescriptor packageView, @NotNull StringBuilder builder) {
        builder.append(renderKeyword("package")).append(" ");
        builder.append(renderFqName(packageView.getFqName()));
        if (debugMode) {
            builder.append(" in context of ");
            renderName(packageView.getModule(), builder);
        }
    }

    private void renderPackageFragment(@NotNull PackageFragmentDescriptor fragment, @NotNull StringBuilder builder) {
        builder.append(renderKeyword("package-fragment")).append(" ");
        builder.append(renderFqName(fragment.getFqName()));
        if (debugMode) {
            builder.append(" in ");
            renderName(fragment.getContainingDeclaration(), builder);
        }
    }


    /* STUPID DISPATCH-ONLY VISITOR */
    private class RenderDeclarationDescriptorVisitor extends DeclarationDescriptorVisitorEmptyBodies<Void, StringBuilder> {
        @Override
        public Void visitValueParameterDescriptor(ValueParameterDescriptor descriptor, StringBuilder builder) {
            renderValueParameter(descriptor, builder, true);
            return null;
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            renderVariable(descriptor, builder, true);
            return null;
        }

        @Override
        public Void visitPropertyDescriptor(PropertyDescriptor descriptor, StringBuilder builder) {
            renderProperty(descriptor, builder);
            return null;
        }

        @Override
        public Void visitFunctionDescriptor(FunctionDescriptor descriptor, StringBuilder builder) {
            renderFunction(descriptor, builder);
            return null;
        }

        @Override
        public Void visitReceiverParameterDescriptor(ReceiverParameterDescriptor descriptor, StringBuilder data) {
            throw new UnsupportedOperationException("Don't render receiver parameters");
        }

        @Override
        public Void visitConstructorDescriptor(ConstructorDescriptor constructorDescriptor, StringBuilder builder) {
            renderConstructor(constructorDescriptor, builder);
            return null;
        }

        @Override
        public Void visitTypeParameterDescriptor(TypeParameterDescriptor descriptor, StringBuilder builder) {
            renderTypeParameter(descriptor, builder, true);
            return null;
        }

        @Override
        public Void visitPackageFragmentDescriptor(
                PackageFragmentDescriptor descriptor, StringBuilder builder
        ) {
            renderPackageFragment(descriptor, builder);
            return null;
        }

        @Override
        public Void visitPackageViewDescriptor(
                PackageViewDescriptor descriptor, StringBuilder builder
        ) {
            renderPackageView(descriptor, builder);
            return null;
        }

        @Override
        public Void visitModuleDeclaration(ModuleDescriptor descriptor, StringBuilder builder) {
            renderModuleOrScript(descriptor, builder);
            return null;
        }

        @Override
        public Void visitScriptDescriptor(ScriptDescriptor scriptDescriptor, StringBuilder builder) {
            renderModuleOrScript(scriptDescriptor, builder);
            return null;
        }

        @Override
        public Void visitClassDescriptor(ClassDescriptor descriptor, StringBuilder builder) {
            renderClass(descriptor, builder);
            return null;
        }
   }
}
