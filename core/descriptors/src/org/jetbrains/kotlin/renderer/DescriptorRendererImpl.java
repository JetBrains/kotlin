/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.renderer;

import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.builtins.KotlinBuiltIns;
import org.jetbrains.kotlin.descriptors.*;
import org.jetbrains.kotlin.descriptors.annotations.Annotated;
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor;
import org.jetbrains.kotlin.descriptors.annotations.DefaultAnnotationArgumentVisitor;
import org.jetbrains.kotlin.descriptors.impl.DeclarationDescriptorVisitorEmptyBodies;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.name.FqNameBase;
import org.jetbrains.kotlin.name.FqNameUnsafe;
import org.jetbrains.kotlin.name.Name;
import org.jetbrains.kotlin.resolve.DescriptorUtils;
import org.jetbrains.kotlin.resolve.constants.AnnotationValue;
import org.jetbrains.kotlin.resolve.constants.ArrayValue;
import org.jetbrains.kotlin.resolve.constants.CompileTimeConstant;
import org.jetbrains.kotlin.resolve.constants.JavaClassValue;
import org.jetbrains.kotlin.types.*;
import org.jetbrains.kotlin.types.ErrorUtils.UninferredParameterTypeConstructor;
import org.jetbrains.kotlin.types.error.MissingDependencyErrorClass;
import org.jetbrains.kotlin.utils.UtilsPackage;

import java.util.*;

import static org.jetbrains.kotlin.resolve.DescriptorUtils.isDefaultObject;
import static org.jetbrains.kotlin.types.TypeUtils.CANT_INFER_FUNCTION_PARAM_TYPE;

public class DescriptorRendererImpl implements DescriptorRenderer {

    private final Function1<JetType, JetType> typeNormalizer;
    private final NameShortness nameShortness;
    private final boolean withDefinedIn;
    private final Set<DescriptorRenderer.Modifier> modifiers;
    private final boolean startFromName;
    private final boolean debugMode;
    private final boolean classWithPrimaryConstructor;
    private final boolean verbose;
    private final boolean unitReturnType;
    private final boolean normalizedVisibilities;
    private final boolean showInternalKeyword;
    private final boolean prettyFunctionTypes;
    private final boolean uninferredTypeParameterAsName;
    private final boolean includeSynthesizedParameterNames;
    private final boolean withoutFunctionParameterNames;
    private final boolean withoutTypeParameters;
    private final boolean renderDefaultObjectName;
    private final boolean withoutSuperTypes;
    private final boolean receiverAfterName;
    private final boolean renderDefaultValues;
    private final boolean flexibleTypesForCode;

    @NotNull
    private final OverrideRenderingPolicy overrideRenderingPolicy;
    @NotNull
    private final ValueParametersHandler handler;
    @NotNull
    private final TextFormat textFormat;
    private final boolean includePropertyConstant;
    private final boolean secondaryConstructorsAsPrimary;
    @NotNull
    private final Set<FqName> excludedAnnotationClasses;

    /* package */ DescriptorRendererImpl(
            NameShortness nameShortness,
            boolean withDefinedIn,
            Set<Modifier> modifiers,
            boolean startFromName,
            boolean debugMode,
            boolean classWithPrimaryConstructor,
            boolean verbose,
            boolean unitReturnType,
            boolean normalizedVisibilities,
            boolean showInternalKeyword,
            boolean prettyFunctionTypes,
            boolean uninferredTypeParameterAsName,
            @NotNull OverrideRenderingPolicy overrideRenderingPolicy,
            @NotNull ValueParametersHandler handler,
            @NotNull TextFormat textFormat,
            @NotNull Collection<FqName> excludedAnnotationClasses,
            boolean includePropertyConstant,
            boolean includeSynthesizedParameterNames,
            boolean withoutFunctionParameterNames,
            boolean withoutTypeParameters,
            boolean receiverAfterName,
            boolean renderDefaultObjectName,
            boolean withoutSuperTypes,
            @NotNull Function1<JetType, JetType> typeNormalizer,
            boolean renderDefaultValues,
            boolean flexibleTypesForCode,
            boolean secondaryConstructorsAsPrimary
    ) {
        this.nameShortness = nameShortness;
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
        this.includePropertyConstant = includePropertyConstant;
        this.secondaryConstructorsAsPrimary = secondaryConstructorsAsPrimary;
        this.excludedAnnotationClasses = new HashSet<FqName>(excludedAnnotationClasses);
        this.prettyFunctionTypes = prettyFunctionTypes;
        this.uninferredTypeParameterAsName = uninferredTypeParameterAsName;
        this.includeSynthesizedParameterNames = includeSynthesizedParameterNames;
        this.withoutFunctionParameterNames = withoutFunctionParameterNames;
        this.withoutTypeParameters = withoutTypeParameters;
        this.receiverAfterName = receiverAfterName;
        this.renderDefaultObjectName = renderDefaultObjectName;
        this.withoutSuperTypes = withoutSuperTypes;
        this.typeNormalizer = typeNormalizer;
        this.renderDefaultValues = renderDefaultValues;
        this.flexibleTypesForCode = flexibleTypesForCode;
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
    private String renderError(@NotNull String keyword) {
        switch (textFormat) {
            case PLAIN:
                return keyword;
            case HTML:
                return "<font color=red><b>" + keyword + "</b></font>";
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
    private String gt() {
        return escape(">");
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

    private static void renderSpaceIfNeeded(@NotNull StringBuilder builder) {
        int length = builder.length();
        if (length == 0 || builder.charAt(length - 1) != ' ') {
            builder.append(' ');
        }
    }

    /* NAMES RENDERING */
    @Override
    @NotNull
    public String renderName(@NotNull Name identifier) {
        String asString = identifier.asString();
        return escape(nameShouldBeEscaped(identifier) ? '`' + asString + '`' : asString);
    }

    private static boolean nameShouldBeEscaped(@NotNull Name identifier) {
        if (identifier.isSpecial()) return false;

        String name = identifier.asString();

        if (KeywordStringsGenerated.KEYWORDS.contains(name)) return true;

        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!Character.isLetterOrDigit(c) && c != '_') return true;
        }

        return false;
    }

    private void renderName(@NotNull DeclarationDescriptor descriptor, @NotNull StringBuilder builder) {
        builder.append(renderName(descriptor.getName()));
    }

    private void renderDefaultObjectName(@NotNull DeclarationDescriptor descriptor, @NotNull StringBuilder builder) {
        if (renderDefaultObjectName) {
            if (startFromName) {
                builder.append("default object");
            }
            renderSpaceIfNeeded(builder);
            DeclarationDescriptor containingDeclaration = descriptor.getContainingDeclaration();
            if (containingDeclaration != null) {
                builder.append("of ");
                builder.append(renderName(containingDeclaration.getName()));
            }
        }
        if (verbose) {
            if (!startFromName) renderSpaceIfNeeded(builder);
            builder.append(renderName(descriptor.getName()));
        }
    }

    @Override
    @NotNull
    public String renderFqName(@NotNull FqNameBase fqName) {
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

    @Override
    @NotNull
    public String renderClassifierName(@NotNull ClassifierDescriptor klass) {
        if (klass instanceof MissingDependencyErrorClass) {
            return ((MissingDependencyErrorClass) klass).getFullFqName().asString();
        }
        if (ErrorUtils.isError(klass)) {
            return klass.getTypeConstructor().toString();
        }
        switch (nameShortness) {
            case SHORT: {
                List<Name> qualifiedNameElements = new ArrayList<Name>();

                // for nested classes qualified name should be used
                DeclarationDescriptor current = klass;
                do {
                    qualifiedNameElements.add(current.getName());
                    current = current.getContainingDeclaration();
                }
                while (current instanceof ClassDescriptor);

                Collections.reverse(qualifiedNameElements);
                return renderFqName(qualifiedNameElements);
            }

            case FULLY_QUALIFIED:
                return renderFqName(DescriptorUtils.getFqName(klass));

            case SOURCE_CODE_QUALIFIED:
                return RendererPackage.qualifiedNameForSourceCode(klass);

            default:
                throw new IllegalArgumentException();
        }
    }

    /* TYPES RENDERING */
    @NotNull
    @Override
    public String renderType(@NotNull JetType type) {
        return renderNormalizedType(typeNormalizer.invoke(type));
    }

    @NotNull
    private String renderNormalizedType(@NotNull JetType type) {
        if (type instanceof LazyType && debugMode) {
            return type.toString();
        }
        if (TypesPackage.isDynamic(type)) {
            return "dynamic";
        }
        if (TypesPackage.isFlexible(type)) {
            if (debugMode) {
                return renderFlexibleTypeWithBothBounds(TypesPackage.flexibility(type).getLowerBound(),
                                                        TypesPackage.flexibility(type).getUpperBound());
            }
            else if (flexibleTypesForCode) {
                String prefix = nameShortness == NameShortness.SHORT ? "" : Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getPackageFqName().asString() + ".";
                return prefix + Flexibility.FLEXIBLE_TYPE_CLASSIFIER.getRelativeClassName()
                       + lt()
                       + renderNormalizedType(TypesPackage.flexibility(type).getLowerBound()) + ", "
                       + renderNormalizedType(TypesPackage.flexibility(type).getUpperBound())
                       + gt();
            }
            else {
                return renderFlexibleType(type);
            }
        }
        return renderInflexibleType(type);
    }

    private String renderFlexibleTypeWithBothBounds(@NotNull JetType lower, @NotNull JetType upper) {
        return "(" + renderNormalizedType(lower) + ".." + renderNormalizedType(upper) + ")";
    }

    private String renderInflexibleType(@NotNull JetType type) {
        assert !TypesPackage.isFlexible(type) : "Flexible types not allowed here: " + renderNormalizedType(type);

        if (type == CANT_INFER_FUNCTION_PARAM_TYPE || TypeUtils.isDontCarePlaceholder(type)) {
            return "???";
        }
        if (ErrorUtils.isUninferredParameter(type)) {
            if (uninferredTypeParameterAsName) {
                return renderError(((UninferredParameterTypeConstructor) type.getConstructor()).getTypeParameterDescriptor().getName().toString());
            }
            return "???";
        }
        if (type.isError()) {
            return renderDefaultType(type);
        }
        if (shouldRenderAsPrettyFunctionType(type)) {
            return renderFunctionType(type);
        }
        return renderDefaultType(type);
    }

    private boolean shouldRenderAsPrettyFunctionType(@NotNull JetType type) {
        return KotlinBuiltIns.isExactFunctionOrExtensionFunctionType(type) && prettyFunctionTypes;
    }

    @NotNull
    private String renderFlexibleType(@NotNull JetType type) {
        JetType lower = TypesPackage.flexibility(type).getLowerBound();
        JetType upper = TypesPackage.flexibility(type).getUpperBound();

        String lowerRendered = renderInflexibleType(lower);
        String upperRendered = renderInflexibleType(upper);

        if (differsOnlyInNullability(lowerRendered, upperRendered)) {
            if (upperRendered.startsWith("(")) {
                // the case of complex type, e.g. (() -> Unit)?
                return "(" + lowerRendered + ")!";
            }
            return lowerRendered + "!";
        }

        String kotlinPrefix = nameShortness != NameShortness.SHORT ? "kotlin." : "";
        String mutablePrefix = "Mutable";
        // java.util.List<Foo> -> (Mutable)List<Foo!>!
        String simpleCollection = replacePrefixes(
                lowerRendered, kotlinPrefix + mutablePrefix, upperRendered, kotlinPrefix, kotlinPrefix + "(" + mutablePrefix + ")"
        );
        if (simpleCollection != null) return simpleCollection;
        // java.util.Map.Entry<Foo, Bar> -> (Mutable)Map.(Mutable)Entry<Foo!, Bar!>!
        String mutableEntry = replacePrefixes(
                lowerRendered, kotlinPrefix + "MutableMap.MutableEntry", upperRendered, kotlinPrefix + "Map.Entry",
                kotlinPrefix + "(Mutable)Map.(Mutable)Entry"
        );
        if (mutableEntry != null) return mutableEntry;

        // Foo[] -> Array<(out) Foo!>!
        String array = replacePrefixes(
                lowerRendered, kotlinPrefix + escape("Array<"), upperRendered, kotlinPrefix + escape("Array<out "),
                kotlinPrefix + escape("Array<(out) ")
        );
        if (array != null) return array;
        return renderFlexibleTypeWithBothBounds(lower, upper);
    }

    @Nullable
    private static String replacePrefixes(
            @NotNull String lowerRendered,
            @NotNull String lowerPrefix,
            @NotNull String upperRendered,
            @NotNull String upperPrefix,
            @NotNull String foldedPrefix
    ) {
        if (lowerRendered.startsWith(lowerPrefix) && upperRendered.startsWith(upperPrefix)) {
            String lowerWithoutPrefix = lowerRendered.substring(lowerPrefix.length());
            if (differsOnlyInNullability(lowerWithoutPrefix, upperRendered.substring(upperPrefix.length()))) {
                return foldedPrefix + lowerWithoutPrefix + "!";
            }
        }
        return null;
    }

    private static boolean differsOnlyInNullability(String lower, String upper) {
        return lower.equals(upper.replace("?", ""))
               || upper.endsWith("?") && ((lower + "?").equals(upper)) || (("(" + lower + ")?").equals(upper));
    }

    @NotNull
    @Override
    public String renderTypeArguments(@NotNull List<TypeProjection> typeArguments) {
        if (typeArguments.isEmpty()) return "";
        StringBuilder sb = new StringBuilder();
        sb.append(lt());
        appendTypeProjections(typeArguments, sb);
        sb.append(gt());
        return sb.toString();
    }

    @NotNull
    private String renderDefaultType(@NotNull JetType type) {
        StringBuilder sb = new StringBuilder();

        if (type.isError()) {
            sb.append(type.getConstructor().toString()); // Debug name of an error type is more informative
        }
        else {
            sb.append(renderTypeName(type.getConstructor()));
        }
        sb.append(renderTypeArguments(type.getArguments()));
        if (type.isMarkedNullable()) {
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
            return renderClassifierName((ClassDescriptor) cd);
        }
        else {
            assert cd == null: "Unexpected classifier: " + cd.getClass();
            return typeConstructor.toString();
        }
    }

    private void appendTypeProjections(@NotNull List<TypeProjection> typeProjections, @NotNull StringBuilder builder) {
        for (Iterator<TypeProjection> iterator = typeProjections.iterator(); iterator.hasNext(); ) {
            TypeProjection typeProjection = iterator.next();
            if (typeProjection.isStarProjection()) {
                builder.append("*");
            }
            else {
                if (typeProjection.getProjectionKind() != Variance.INVARIANT) {
                    builder.append(typeProjection.getProjectionKind()).append(" ");
                }
                builder.append(renderNormalizedType(typeProjection.getType()));
            }
            if (iterator.hasNext()) {
                builder.append(", ");
            }
        }
    }

    @NotNull
    private String renderFunctionType(@NotNull JetType type) {
        StringBuilder sb = new StringBuilder();

        JetType receiverType = KotlinBuiltIns.getReceiverType(type);
        if (receiverType != null) {
            sb.append(renderNormalizedType(receiverType));
            sb.append(".");
        }

        sb.append("(");
        appendTypeProjections(KotlinBuiltIns.getParameterTypeProjectionsFromFunctionType(type), sb);
        sb.append(") ").append(arrow()).append(" ");
        sb.append(renderNormalizedType(KotlinBuiltIns.getReturnTypeFromFunctionType(type)));

        if (type.isMarkedNullable()) {
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
                builder.append(renderAnnotation(annotation)).append(" ");
            }
        }
    }

    @Override
    @NotNull
    public String renderAnnotation(@NotNull AnnotationDescriptor annotation) {
        StringBuilder sb = new StringBuilder();
        sb.append(renderType(annotation.getType()));
        if (verbose) {
            sb.append("(").append(UtilsPackage.join(renderAndSortAnnotationArguments(annotation), ", ")).append(")");
        }
        return sb.toString();
    }

    @NotNull
    private List<String> renderAndSortAnnotationArguments(@NotNull AnnotationDescriptor descriptor) {
        Set<Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>>> valueArguments = descriptor.getAllValueArguments().entrySet();
        List<String> resultList = new ArrayList<String>(valueArguments.size());
        for (Map.Entry<ValueParameterDescriptor, CompileTimeConstant<?>> entry : valueArguments) {
            CompileTimeConstant<?> value = entry.getValue();
            String typeSuffix = ": " + renderType(value.getType(KotlinBuiltIns.getInstance()));
            resultList.add(entry.getKey().getName().asString() + " = " + renderConstant(value) + typeSuffix);
        }
        Collections.sort(resultList);
        return resultList;
    }

    @NotNull
    private String renderConstant(@NotNull CompileTimeConstant<?> value) {
        return value.accept(
                new DefaultAnnotationArgumentVisitor<String, Void>() {
                    @Override
                    public String visitValue(@NotNull CompileTimeConstant<?> value, Void data) {
                        return value.toString();
                    }

                    @Override
                    public String visitArrayValue(ArrayValue value, Void data) {
                        List<String> renderedElements =
                                KotlinPackage.map(value.getValue(),
                                                  new Function1<CompileTimeConstant<?>, String>() {
                                                      @Override
                                                      public String invoke(CompileTimeConstant<?> constant) {
                                                          return renderConstant(constant);
                                                      }
                                                  });
                        return "{" + UtilsPackage.join(renderedElements, ", ") + "}";
                    }

                    @Override
                    public String visitAnnotationValue(AnnotationValue value, Void data) {
                        return renderAnnotation(value.getValue());
                    }

                    @Override
                    public String visitJavaClassValue(JavaClassValue value, Void data) {
                        return renderType(value.getValue()) + ".class";
                    }
                },
                null
        );
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

    private static boolean overridesSomething(CallableMemberDescriptor callable) {
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
        String variance = typeParameter.getVariance().getLabel();
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
            builder.append(gt());
        }
    }

    private void renderTypeParameters(
            @NotNull List<TypeParameterDescriptor> typeParameters,
            @NotNull StringBuilder builder,
            boolean withSpace
    ) {
        if (withoutTypeParameters) return;

        if (!typeParameters.isEmpty()) {
            builder.append(lt());
            for (Iterator<TypeParameterDescriptor> iterator = typeParameters.iterator(); iterator.hasNext(); ) {
                TypeParameterDescriptor typeParameterDescriptor = iterator.next();
                renderTypeParameter(typeParameterDescriptor, builder, false);
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(gt());
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
            renderReceiver(function, builder);
        }

        renderName(function, builder);

        renderValueParameters(function, builder);

        renderReceiverAfterName(function, builder);

        JetType returnType = function.getReturnType();
        if (unitReturnType || (returnType == null || !KotlinBuiltIns.isUnit(returnType))) {
            builder.append(": ").append(returnType == null ? "[NULL]" : escape(renderType(returnType)));
        }

        renderWhereSuffix(function.getTypeParameters(), builder);
    }

    private void renderReceiverAfterName(CallableDescriptor callableDescriptor, StringBuilder builder) {
        if (!receiverAfterName) return;

        ReceiverParameterDescriptor receiver = callableDescriptor.getExtensionReceiverParameter();
        if (receiver != null) {
            builder.append(" on ").append(escape(renderType(receiver.getType())));
        }
    }

    private void renderReceiver(CallableDescriptor callableDescriptor, StringBuilder builder) {
        ReceiverParameterDescriptor receiver = callableDescriptor.getExtensionReceiverParameter();
        if (receiver != null) {
            JetType type = receiver.getType();
            String result = escape(renderType(type));
            if (shouldRenderAsPrettyFunctionType(type) && !TypeUtils.isNullableType(type)) {
                result = "(" + result + ")";
            }
            builder.append(result).append(".");
        }
    }

    private void renderConstructor(@NotNull ConstructorDescriptor constructor, @NotNull StringBuilder builder) {
        renderAnnotations(constructor, builder);
        renderVisibility(constructor.getVisibility(), builder);
        renderMemberKind(constructor, builder);

        builder.append(renderKeyword("constructor"));
        if (secondaryConstructorsAsPrimary) {
            ClassDescriptor classDescriptor = constructor.getContainingDeclaration();
            builder.append(" ");
            renderName(classDescriptor, builder);
            renderTypeParameters(classDescriptor.getTypeConstructor().getParameters(), builder, false);
        }

        renderValueParameters(constructor, builder);

        if (secondaryConstructorsAsPrimary) {
            renderWhereSuffix(constructor.getTypeParameters(), builder);
        }
    }

    private void renderWhereSuffix(@NotNull List<TypeParameterDescriptor> typeParameters, @NotNull StringBuilder builder) {
        if (withoutTypeParameters) return;

        List<String> upperBoundStrings = new ArrayList<String>(0);

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
            builder.append(UtilsPackage.join(upperBoundStrings, ", "));
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
        boolean includeNames = !withoutFunctionParameterNames &&
                               (includeSynthesizedParameterNames || !function.hasSynthesizedParameterNames());
        handler.appendBeforeValueParameters(function, builder);
        for (ValueParameterDescriptor parameter : function.getValueParameters()) {
            handler.appendBeforeValueParameter(parameter, builder);
            renderValueParameter(parameter, includeNames, builder, false);
            handler.appendAfterValueParameter(parameter, builder);
        }
        handler.appendAfterValueParameters(function, builder);
    }

    /* VARIABLES */
    private void renderValueParameter(@NotNull ValueParameterDescriptor valueParameter, boolean includeName, @NotNull StringBuilder builder, boolean topLevel) {
        if (topLevel) {
            builder.append(renderKeyword("value-parameter")).append(" ");
        }

        if (verbose) {
            builder.append("/*").append(valueParameter.getIndex()).append("*/ ");
        }

        renderAnnotations(valueParameter, builder);
        renderVariable(valueParameter, includeName, builder, topLevel);
        boolean withDefaultValue = renderDefaultValues && (debugMode ? valueParameter.declaresDefaultValue() : valueParameter.hasDefaultValue());
        if (withDefaultValue) {
            builder.append(" = ...");
        }
    }

    private void renderValVarPrefix(@NotNull VariableDescriptor variable, @NotNull StringBuilder builder) {
        builder.append(renderKeyword(variable.isVar() ? "var" : "val")).append(" ");
    }

    private void renderVariable(@NotNull VariableDescriptor variable, boolean includeName, @NotNull StringBuilder builder, boolean topLevel) {
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

        if (includeName) {
            renderName(variable, builder);
            builder.append(": ");
        }

        builder.append(escape(renderType(typeToRender)));

        renderInitializer(variable, builder);

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
            renderTypeParameters(property.getTypeParameters(), builder, true);
            renderReceiver(property, builder);
        }

        renderName(property, builder);
        builder.append(": ").append(escape(renderType(property.getType())));

        renderReceiverAfterName(property, builder);

        renderInitializer(property, builder);

        renderWhereSuffix(property.getTypeParameters(), builder);
    }

    private void renderInitializer(@NotNull VariableDescriptor variable, @NotNull StringBuilder builder) {
        if (includePropertyConstant) {
            CompileTimeConstant<?> initializer = variable.getCompileTimeInitializer();
            if (initializer != null) {
                builder.append(" = ").append(escape(renderConstant(initializer)));
            }
        }
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
            renderClassKindPrefix(klass, builder);
        }

        if (!isDefaultObject(klass)) {
            if (!startFromName) renderSpaceIfNeeded(builder);
            renderName(klass, builder);
        }
        else {
            renderDefaultObjectName(klass, builder);
        }

        List<TypeParameterDescriptor> typeParameters = klass.getTypeConstructor().getParameters();
        renderTypeParameters(typeParameters, builder, false);

        if (!klass.getKind().isSingleton() && classWithPrimaryConstructor) {
            ConstructorDescriptor primaryConstructor = klass.getUnsubstitutedPrimaryConstructor();
            if (primaryConstructor != null) {
                renderValueParameters(primaryConstructor, builder);
            }
        }

        renderSuperTypes(klass, builder);
        renderWhereSuffix(typeParameters, builder);
    }

    private void renderSuperTypes(@NotNull ClassDescriptor klass, @NotNull StringBuilder builder) {
        if (withoutSuperTypes) return;

        if (!klass.equals(KotlinBuiltIns.getInstance().getNothing())) {
            Collection<JetType> supertypes = klass.getTypeConstructor().getSupertypes();

            if (supertypes.isEmpty() ||
                supertypes.size() == 1 && KotlinBuiltIns.isAnyOrNullableAny(supertypes.iterator().next())) {
            }
            else {
                renderSpaceIfNeeded(builder);
                builder.append(": ");
                for (Iterator<JetType> iterator = supertypes.iterator(); iterator.hasNext(); ) {
                    JetType supertype = iterator.next();
                    builder.append(renderType(supertype));
                    if (iterator.hasNext()) {
                        builder.append(", ");
                    }
                }
            }
        }
    }

    private void renderClassKindPrefix(ClassDescriptor klass, StringBuilder builder) {
        builder.append(renderKeyword(getClassKindPrefix(klass)));
    }

    @NotNull
    public static String getClassKindPrefix(@NotNull ClassDescriptor klass) {
        if (klass.isDefaultObject()) {
            return "default object";
        }
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
            renderValueParameter(descriptor, true, builder, true);
            return null;
        }

        @Override
        public Void visitVariableDescriptor(VariableDescriptor descriptor, StringBuilder builder) {
            renderVariable(descriptor, true, builder, true);
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
