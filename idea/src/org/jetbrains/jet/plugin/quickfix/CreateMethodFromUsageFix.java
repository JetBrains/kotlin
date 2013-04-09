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

package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.codeInsight.lookup.LookupElement;
import com.intellij.codeInsight.lookup.LookupElementBuilder;
import com.intellij.codeInsight.template.*;
import com.intellij.codeInsight.template.impl.TemplateImpl;
import com.intellij.codeInsight.template.impl.Variable;
import com.intellij.ide.fileTemplates.FileTemplate;
import com.intellij.ide.fileTemplates.FileTemplateManager;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.TypeProjection;
import org.jetbrains.jet.lang.types.TypeUtils;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.DescriptorToDeclarationUtil;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import java.util.*;

public class CreateMethodFromUsageFix extends CreateFromUsageFixBase {
    private static final String TYPE_PARAMETER_LIST_VARIABLE_NAME = "typeParameterList";
    private static final String TEMPLATE_FROM_USAGE_METHOD_BODY = "New Kotlin Method Body.kt";

    /**
     * Represents a concrete type or a set of types yet to be inferred from an expression.
     */
    private static class TypeOrExpressionThereof {
        private final JetExpression expressionOfType;
        private final JetType type;
        private JetType[] cachedTypeCandidates;
        private String[] cachedNameCandidatesFromExpression;

        public TypeOrExpressionThereof(@NotNull JetExpression expressionOfType) {
            this.expressionOfType = expressionOfType;
            this.type = null;
        }

        public TypeOrExpressionThereof(@NotNull JetType type) {
            this.expressionOfType = null;
            this.type = type;
        }

        public boolean isType() {
            return this.type != null;
        }

        @Nullable
        public JetType getType() {
            return this.type;
        }

        @Nullable
        public JetExpression getExpressionOfType() {
            return expressionOfType;
        }

        /**
         * Returns a collection containing the possible types represented by this instance. Infers the type from an expression if necessary.
         * @return A collection containing the possible types represented by this instance.
         */
        @NotNull
        public JetType[] getPossibleTypes(@Nullable("used cached, don't recompute") BindingContext context) {
            if (context == null) {
                assert cachedTypeCandidates != null;
                return cachedTypeCandidates;
            }
            List<JetType> types = new ArrayList<JetType>();
            if (isType()) {
                types.add(type);
                types.addAll(TypeUtils.getAllSupertypes(type));
            } else {
                for (JetType type : guessTypeForExpression(expressionOfType, context)) {
                    types.add(type);
                    types.addAll(TypeUtils.getAllSupertypes(type));
                }
            }
            return cachedTypeCandidates = types.toArray(new JetType[types.size()]);
        }

        @NotNull
        public JetType[] getPossibleTypes() {
            return getPossibleTypes(null);
        }

        @NotNull
        public String[] getPossibleNamesFromExpression() {
            if (cachedNameCandidatesFromExpression != null) return cachedNameCandidatesFromExpression;
            cachedNameCandidatesFromExpression = isType()
                                                 ? ArrayUtil.EMPTY_STRING_ARRAY
                                                 : JetNameSuggester.suggestNamesForExpression(
                                                         expressionOfType,
                                                         JetNameValidator.getEmptyValidator(expressionOfType.getProject()));
            return cachedNameCandidatesFromExpression;
        }
    }

    /**
     * Encapsulates information about a method parameter that is going to be created.
     */
    private static class Parameter {
        private final String preferredName;
        private final TypeOrExpressionThereof type;

        public Parameter(@Nullable("no preferred name") String preferredName, TypeOrExpressionThereof type) {
            this.preferredName = preferredName;
            this.type = type;
        }

        public String getPreferredName() {
            return preferredName;
        }

        public TypeOrExpressionThereof getType() {
            return type;
        }
    }

    /**
     * Special <code>Expression</code> for parameter names based on its type.
     */
    private static class ParameterNameExpression extends Expression {
        private final String[] names;
        private final Map<String, String[]> parameterTypeToNamesMap;

        public ParameterNameExpression(@NotNull String[] names, @NotNull Map<String, String[]> parameterTypeToNamesMap) {
            for (String name : names)
                assert name != null && !name.isEmpty();
            this.names = names;
            this.parameterTypeToNamesMap = parameterTypeToNamesMap;
        }

        @Nullable
        @Override
        public Result calculateResult(ExpressionContext context) {
            LookupElement[] lookupItems = calculateLookupItems(context);
            if (lookupItems.length == 0) return new TextResult("");

            return new TextResult(lookupItems[0].getLookupString());
        }

        @Nullable
        @Override
        public Result calculateQuickResult(ExpressionContext context) {
            return null;
        }

        @NotNull
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            Set<String> names = new LinkedHashSet<String>();
            Collections.addAll(names, this.names);

            // find the parameter list
            Project project = context.getProject();
            int offset = context.getStartOffset();
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            Editor editor = context.getEditor();
            assert editor != null;
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            assert file != null && file instanceof JetFile;
            PsiElement elementAt = file.findElementAt(offset);
            JetFunction func = PsiTreeUtil.getParentOfType(elementAt, JetFunction.class);
            assert func != null;
            JetParameterList parameterList = func.getValueParameterList();
            assert parameterList != null;

            // add names based on selected type
            JetParameter parameter = PsiTreeUtil.getParentOfType(elementAt, JetParameter.class);
            if (parameter != null) {
                JetTypeReference parameterTypeRef = parameter.getTypeReference();
                assert parameterTypeRef != null;
                String[] suggestedNamesBasedOnType = parameterTypeToNamesMap.get(parameterTypeRef.getText());
                if (suggestedNamesBasedOnType != null) {
                    Collections.addAll(names, suggestedNamesBasedOnType);
                }
            }

            // remember other parameter names for later use
            Set<String> parameterNames = new HashSet<String>();
            for (JetParameter jetParameter : parameterList.getParameters()) {
                if (jetParameter == parameter) continue;
                parameterNames.add(jetParameter.getName());
            }

            // add fallback parameter name
            if (names.isEmpty()) {
                names.add("arg");
            }

            // ensure there are no conflicts
            List<LookupElement> lookupElements = new ArrayList<LookupElement>();
            for (String name : names) {
                name = getNextAvailableName(name, parameterNames);
                lookupElements.add(LookupElementBuilder.create(name));
            }

            // create and return
            return lookupElements.toArray(new LookupElement[lookupElements.size()]);
        }
    }

    /**
     * An <code>Expression</code> for type references.
     */
    private static class TypeExpression extends Expression {
        private final JetType[] options;
        private final String[] optionStrings;

        public TypeExpression(@NotNull JetType[] options) {
            //To change body of created methods use File | Settings | File Templates.
            this.options = options;
            optionStrings = renderTypes(options);
        }

        @Nullable
        @Override
        public Result calculateResult(ExpressionContext context) {
            LookupElement[] lookupItems = calculateLookupItems(context);
            if (lookupItems.length == 0) return new TextResult("");

            return new TextResult(lookupItems[0].getLookupString());
        }

        @Nullable
        @Override
        public Result calculateQuickResult(ExpressionContext context) {
            return null;
        }

        @NotNull
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            LookupElement[] lookupElements = new LookupElement[options.length];
            for (int i = 0; i < options.length; i++) {
                lookupElements[i] = LookupElementBuilder.create(optionStrings[i]);
            }
            return lookupElements;
        }

        @NotNull
        public JetType[] getOptions() {
            return options;
        }

        @NotNull
        public String[] getOptionStrings() {
            return optionStrings;
        }
    }

    /**
     * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
     */
    private static class TypeParameterListExpression extends Expression {
        private final String[] ownerTypeParameterNames;
        private final Map<String, String[]> typeParameterMap;

        public TypeParameterListExpression(@NotNull String[] ownerTypeParameterNames, @NotNull Map<String, String[]> typeParameterMap) {
            this.ownerTypeParameterNames = ownerTypeParameterNames;
            this.typeParameterMap = typeParameterMap;
        }

        @NotNull
        @Override
        public Result calculateResult(ExpressionContext context) {
            Project project = context.getProject();
            int offset = context.getStartOffset();
            PsiDocumentManager.getInstance(project).commitAllDocuments();
            Editor editor = context.getEditor();
            assert editor != null;
            PsiFile file = PsiDocumentManager.getInstance(project).getPsiFile(editor.getDocument());
            assert file != null && file instanceof JetFile;
            PsiElement elementAt = file.findElementAt(offset);
            JetFunction func = PsiTreeUtil.getParentOfType(elementAt, JetFunction.class);
            assert func != null;
            List<JetParameter> parameters = func.getValueParameters();

            List<String> typeParameterNames = new ArrayList<String>();
            Collections.addAll(typeParameterNames, ownerTypeParameterNames);
            for (JetParameter parameter : parameters) {
                JetTypeReference parameterTypeRef = parameter.getTypeReference();
                assert parameterTypeRef != null;
                String[] names = typeParameterMap.get(parameterTypeRef.getText());
                if (names != null) {
                    Collections.addAll(typeParameterNames, names);
                }
            }
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            if (returnTypeRef != null) {
                String[] names = typeParameterMap.get(returnTypeRef.getText());
                if (names != null) {
                    Collections.addAll(typeParameterNames, names);
                }
            }

            return typeParameterNames.isEmpty()
                    ? new TextResult("")
                    : new TextResult(" <" + StringUtil.join(typeParameterNames, ", ") + ">");
        }

        @Nullable
        @Override
        public Result calculateQuickResult(ExpressionContext context) {
            return null;
        }

        @NotNull
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            return new LookupElement[0]; // do not offer the user any choices
        }
    }

    private final String methodName;
    private final TypeOrExpressionThereof ownerType;
    private final TypeOrExpressionThereof returnType;
    private final List<Parameter> parameters;

    public CreateMethodFromUsageFix(@NotNull PsiElement element, @NotNull TypeOrExpressionThereof ownerType, @NotNull String methodName,
            @NotNull TypeOrExpressionThereof returnType, @NotNull List<Parameter> parameters) {
        super(element);
        this.methodName = methodName;
        this.ownerType = ownerType;
        this.returnType = returnType;
        this.parameters = parameters;
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("create.method.from.usage", methodName);
    }

    @Override
    public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        assert file instanceof JetFile;
        JetFile jetFile = (JetFile) file;
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache(jetFile).getBindingContext();

        JetType[] possibleOwnerTypes = ownerType.getPossibleTypes(context);
        assert possibleOwnerTypes.length > 0;

        JetType ownerType;
        if (possibleOwnerTypes.length == 1) {
            ownerType = possibleOwnerTypes[0];
        } else {
            // TODO: class selection
            ownerType = possibleOwnerTypes[0];
        }

        final ClassifierDescriptor ownerTypeDescriptor = ownerType.getConstructor().getDeclarationDescriptor();
        assert ownerTypeDescriptor != null;
        PsiElement typeDeclaration = DescriptorToDeclarationUtil.getDeclaration(jetFile, ownerTypeDescriptor, context);
        JetClass klass = (JetClass) typeDeclaration;

        // create method with placeholder types and parameter names
        String[] parameterStrings = new String[parameters.size()];
        for (int i = 0; i < parameterStrings.length; i++) {
            parameterStrings[i] = "p" + i + ": Any";
        }
        String parametersString = StringUtil.join(parameterStrings,", ");

        final boolean isUnit = returnType.isType() && isUnit(returnType.getType());
        String returnTypeString = isUnit ? "" : ": Any";

        String ownerTypeString;
        String methodText;
        JetNamedFunction func;
        PsiElement owner;
        final JetFile containingFile;
        boolean isExtension = !klass.isWritable();
        if (isExtension) { // create as extension function
            ownerTypeString = renderType(ownerType);
            methodText = String.format("fun %s.%s(%s)%s { }", ownerTypeString, methodName, parametersString, returnTypeString);
            func = JetPsiFactory.createFunction(project, methodText);
            owner = containingFile = jetFile;
            func = (JetNamedFunction) file.add(func);
        } else { // create as method
            methodText = String.format("fun %s(%s)%s { }", methodName, parametersString, returnTypeString);
            func = JetPsiFactory.createFunction(project, methodText);
            owner = klass;
            PsiFile classContainingFile = klass.getContainingFile();
            assert classContainingFile instanceof JetFile;
            containingFile = (JetFile) classContainingFile;
            JetClassBody classBody = klass.getBody();
            assert classBody != null;
            PsiElement rBrace = classBody.getRBrace();
            func = (JetNamedFunction) classBody.addBefore(func, rBrace);
        }

        // TODO: add newlines

        JetParameterList parameterList = func.getValueParameterList();
        assert parameterList != null;

        // build templates
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        final CaretModel caretModel = editor.getCaretModel();
        final int oldOffset = caretModel.getOffset();
        caretModel.moveToOffset(file.getNode().getStartOffset());

        TemplateBuilderImpl builder = new TemplateBuilderImpl(file);
        TypeExpression returnTypeExpression = isUnit ? null : setupReturnTypeTemplate(builder, func, returnType);
        TypeExpression[] parameterTypeExpressions = setupParameterTypeTemplates(project, builder, parameters, parameterList);

        // add a segment for the parameter list
        // Note: because TemplateBuilderImpl does not have a replaceElement overload that takes in both a TextRange and alwaysStopAt, we
        // need to create the segment first and then hack the Expression into the template later. We use this template to update the type
        // parameter list as the user makes selections in the parameter types, and we need alwaysStopAt to be false so the user can't tab to
        // it.
        JetScope scope = getScope(owner, context);
        TypeParameterListExpression expression = setupTypeParameterListTemplate(builder, func, ownerType, parameterTypeExpressions, returnTypeExpression, scope);

        // the template built by TemplateBuilderImpl is ordered by element position, but we want types to be first, so hack it
        final TemplateImpl template = (TemplateImpl) builder.buildInlineTemplate();
        ArrayList<Variable> variables = template.getVariables();
        for (int i = 0; i < parameters.size(); i++) {
            Collections.swap(variables, i * 2, i * 2 + 1);
        }

        // fix up the template to include the expression for the type parameter list
        variables.add(new Variable(TYPE_PARAMETER_LIST_VARIABLE_NAME, expression, expression, false, true));

        // run the template
        TemplateManager.getInstance(project).startTemplate(editor, template, new TemplateEditingAdapter() {
            @Override
            public void templateFinished(Template _, boolean brokenOff) {
                int offset = template.getSegmentOffset(0);
                final JetNamedFunction func = PsiTreeUtil.findElementOfClassAtOffset(containingFile, offset, JetNamedFunction.class, false);
                assert func != null;
                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        setupFunctionBody(project, func, isUnit, ownerTypeDescriptor);
                    }
                });

                caretModel.moveToOffset(oldOffset);
            }
        });
    }

    private void setupFunctionBody(@NotNull Project project, @NotNull JetNamedFunction func, boolean isUnit,
            @NotNull ClassifierDescriptor ownerTypeDescriptor
    ) {
        FileTemplate fileTemplate = FileTemplateManager.getInstance().getCodeTemplate(TEMPLATE_FROM_USAGE_METHOD_BODY);
        Properties properties = new Properties();
        if (isUnit) {
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, "Unit");
        } else {
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            assert returnTypeRef != null;
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, returnTypeRef.getText());
        }
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFQName(ownerTypeDescriptor).getFqName());
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, ownerTypeDescriptor.getName().getName());
        properties.setProperty(FileTemplate.ATTRIBUTE_METHOD_NAME, methodName);

        @NonNls String bodyText;
        try {
            bodyText = fileTemplate.getText(properties);
        } catch (ProcessCanceledException e) {
            throw e;
        } catch (Exception e) {
            throw new IncorrectOperationException("Failed to parse file template", e);
        }
        JetExpression newBodyExpression = JetPsiFactory.createFunctionBody(project, bodyText);
        JetExpression oldBodyExpression = func.getBodyExpression();
        assert oldBodyExpression != null;
        oldBodyExpression.replace(newBodyExpression);
    }

    @NotNull
    private static TypeExpression setupReturnTypeTemplate(@NotNull TemplateBuilder builder, @NotNull JetNamedFunction func,
            @NotNull TypeOrExpressionThereof returnType) {
        JetTypeReference returnTypeRef = func.getReturnTypeRef();
        assert returnTypeRef != null;
        TypeExpression returnTypeExpression = new TypeExpression(returnType.getPossibleTypes());
        builder.replaceElement(returnTypeRef, returnTypeExpression);
        return returnTypeExpression;
    }

    @NotNull
    private static TypeParameterListExpression setupTypeParameterListTemplate(
            @NotNull TemplateBuilderImpl builder,
            @NotNull JetNamedFunction func,
            @NotNull JetType ownerType,
            @NotNull TypeExpression[] parameterTypeExpressions,
            @Nullable TypeExpression returnTypeExpression,
            @NotNull JetScope scope
    ) {
        Map<String, String[]> typeParameterMap = new HashMap<String, String[]>();
        Set<TypeParameterDescriptor> ownerTypeParameters = getTypeParametersInType(ownerType);
        String[] ownerTypeParameterNames = getTypeParameterNamesNotInScope(ownerTypeParameters, scope);
        for (TypeExpression parameterTypeExpression : parameterTypeExpressions) {
            JetType[] parameterTypeOptions = parameterTypeExpression.getOptions();
            String[] parameterTypeOptionStrings = parameterTypeExpression.getOptionStrings();
            assert parameterTypeOptions.length == parameterTypeOptionStrings.length;
            for (int i = 0; i < parameterTypeOptions.length; i++) {
                Set<TypeParameterDescriptor> typeParameters = getTypeParametersInType(parameterTypeOptions[i]);
                typeParameterMap.put(parameterTypeOptionStrings[i], getTypeParameterNamesNotInScope(typeParameters, scope));
            }
        }

        JetTypeReference returnTypeRef = func.getReturnTypeRef();
        if (returnTypeRef != null) {
            assert returnTypeExpression != null;
            JetType[] returnTypeOptions = returnTypeExpression.getOptions();
            String[] returnTypeOptionStrings = returnTypeExpression.getOptionStrings();
            assert returnTypeOptions.length == returnTypeOptionStrings.length;
            for (int i = 0; i < returnTypeOptions.length; i++) {
                Set<TypeParameterDescriptor> typeParameters = getTypeParametersInType(returnTypeOptions[i]);
                typeParameterMap.put(returnTypeOptionStrings[i], getTypeParameterNamesNotInScope(typeParameters, scope));
            }
        }

        builder.replaceElement(func, TextRange.create(3, 3), TYPE_PARAMETER_LIST_VARIABLE_NAME, null, false); // ((3, 3) is after "fun")
        return new TypeParameterListExpression(ownerTypeParameterNames, typeParameterMap);
    }

    @NotNull
    private static JetScope getScope(@NotNull PsiElement owner, @NotNull BindingContext context) {
        DeclarationDescriptor ownerDescriptor = context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, owner);
        assert ownerDescriptor != null;
        if (ownerDescriptor instanceof NamespaceDescriptor) {
            NamespaceDescriptor namespaceDescriptor = (NamespaceDescriptor) ownerDescriptor;
            return namespaceDescriptor.getMemberScope();
        } else {
            assert ownerDescriptor instanceof MutableClassDescriptor;
            MutableClassDescriptor classDescriptor = (MutableClassDescriptor) ownerDescriptor;
            return classDescriptor.getScopeForMemberResolution();
        }
    }

    private static TypeExpression[] setupParameterTypeTemplates(@NotNull Project project, @NotNull TemplateBuilder builder,
            @NotNull List<Parameter> parameters, @NotNull JetParameterList parameterList) {
        List<JetParameter> jetParameters = parameterList.getParameters();
        assert jetParameters.size() == parameters.size();
        TypeExpression[] parameterTypeExpressions = new TypeExpression[parameters.size()];
        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            JetParameter jetParameter = jetParameters.get(i);

            // add parameter type to the template
            JetType[] typeOptions = parameter.getType().getPossibleTypes();
            parameterTypeExpressions[i] = new TypeExpression(typeOptions);
            JetTypeReference parameterTypeRef = jetParameter.getTypeReference();
            assert parameterTypeRef != null;
            builder.replaceElement(parameterTypeRef, parameterTypeExpressions[i]);

            // add parameter name to the template
            String[] possibleNamesFromExpression = parameter.getType().getPossibleNamesFromExpression();
            String preferredName = parameter.getPreferredName();
            String[] possibleNames;
            if (preferredName != null) {
                possibleNames = new String[possibleNamesFromExpression.length + 1];
                possibleNames[0] = preferredName;
                System.arraycopy(possibleNamesFromExpression, 0, possibleNames, 1, possibleNamesFromExpression.length);
            } else {
                possibleNames = possibleNamesFromExpression;
            }

            // figure out suggested names for each type option
            Map<String, String[]> parameterTypeToNamesMap = new HashMap<String, String[]>();
            String[] typeOptionStrings = parameterTypeExpressions[i].getOptionStrings();
            assert typeOptions.length == typeOptionStrings.length;
            for (int j = 0; j < typeOptions.length; j++) {
                String[] suggestedNames = JetNameSuggester.suggestNamesForType(typeOptions[j], JetNameValidator.getEmptyValidator(project));
                parameterTypeToNamesMap.put(typeOptionStrings[j], suggestedNames);
            }

            // add expression to builder
            Expression parameterNameExpression = new ParameterNameExpression(possibleNames, parameterTypeToNamesMap);
            PsiElement parameterNameIdentifier = jetParameter.getNameIdentifier();
            assert parameterNameIdentifier != null;
            builder.replaceElement(parameterNameIdentifier, parameterNameExpression);
        }
        return parameterTypeExpressions;
    }

    @Override
    public boolean isAvailable(@NotNull Project project, Editor editor, PsiFile file) {
        assert file instanceof JetFile;
        JetFile jetFile = (JetFile) file;
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache(jetFile).getBindingContext();

        JetType[] possibleOwnerTypes = ownerType.getPossibleTypes(context);
        if (possibleOwnerTypes.length == 0) return false;
        assertNoUnitTypes(possibleOwnerTypes);
        JetType[] possibleReturnTypes = returnType.getPossibleTypes(context);
        if (!returnType.isType()) { // allow return type to be unit (but not when it's among several options)
            assertNoUnitTypes(possibleReturnTypes);
        }
        if (possibleReturnTypes.length == 0) return false;
        for (Parameter parameter : parameters) {
            JetType[] possibleTypes = parameter.getType().getPossibleTypes(context);
            if (possibleTypes.length == 0) return false;
            assertNoUnitTypes(possibleTypes);
        }

        return true;
    }

    @NotNull
    private static String renderType(JetType type) {
        return DescriptorRenderer.TEXT.renderType(type);
        // TODO: take into account imports and stuff; how to refer to a type with the simplest name with imports (currently uses fully qualified name)?
    }

    @NotNull
    private static String[] renderTypes(JetType[] types) {
        String[] typeStrings = new String[types.length];
        for (int i = 0; i < types.length; i++) {
            typeStrings[i] = renderType(types[i]);
        }
        return typeStrings;
    }

    @NotNull
    private static String[] getTypeParameterNamesNotInScope(Collection<? extends TypeParameterDescriptor> typeParameters, JetScope scope) {
        List<String> typeParameterNames = new ArrayList<String>();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            ClassifierDescriptor classifier = scope.getClassifier(typeParameter.getName());
            if (classifier == null || !classifier.equals(typeParameter)) {
                typeParameterNames.add(typeParameter.getName().getIdentifier());
            }
        }
        return ArrayUtil.toStringArray(typeParameterNames);
    }

    @NotNull
    private static Set<TypeParameterDescriptor> getTypeParametersInType(@NotNull JetType type) {
        Set<TypeParameterDescriptor> typeParameters = new LinkedHashSet<TypeParameterDescriptor>();
        List<TypeProjection> arguments = type.getArguments();
        if (arguments.isEmpty()) {
            ClassifierDescriptor descriptor = type.getConstructor().getDeclarationDescriptor();
            if (descriptor instanceof TypeParameterDescriptor) {
                typeParameters.add((TypeParameterDescriptor) descriptor);
            }
        } else {
            for (TypeProjection projection : arguments) {
                typeParameters.addAll(getTypeParametersInType(projection.getType()));
            }
        }
        return typeParameters;
    }

    @NotNull
    private static String getNextAvailableName(@NotNull String name, @NotNull Set<String> existingNames) {
        if (existingNames.contains(name)) {
            int j = 1;
            while (existingNames.contains(name + j)) j++;
            name += j;
        }
        return name;
    }

    @NotNull
    private static JetType[] guessTypeForExpression(@NotNull JetExpression expr, @NotNull BindingContext context) {
        JetType actualType = context.get(BindingContext.EXPRESSION_TYPE, expr);
        if (actualType != null) { // if we know the actual type of the expression
            return new JetType[] {actualType};
        }

        // if we need to guess, there are four cases:
        if (expr.getParent() instanceof JetVariableDeclaration) {
            JetVariableDeclaration variable = (JetVariableDeclaration) expr.getParent();
            JetTypeReference variableTypeRef = variable.getTypeRef();
            if (variableTypeRef != null) {
                // case 1: the expression is the RHS of a variable assignment with a specified type
                return new JetType[] {context.get(BindingContext.TYPE, variableTypeRef)};
            } else {
                // case 2: the expression is the RHS of a variable assignment without a specified type
                // TODO
            }
        }

        // TODO: other cases
        return new JetType[0]; //TODO
    }

    private static boolean isUnit(@NotNull JetType type) {
        return KotlinBuiltIns.getInstance().isUnit(type);
    }

    private static void assertNoUnitTypes(@NotNull JetType[] types) {
        for (JetType type : types) {
            assert !isUnit(type) : "no support for unit functions";
        }
    }

    @NotNull
    public static JetIntentionActionFactory createCreateGetMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetArrayAccessExpression accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetArrayAccessExpression.class);
                if (accessExpr == null) return null;
                JetExpression arrayExpr = accessExpr.getArrayExpression();
                TypeOrExpressionThereof arrayType = new TypeOrExpressionThereof(arrayExpr);

                List<Parameter> parameters = new ArrayList<Parameter>();
                for (JetExpression indexExpr : accessExpr.getIndexExpressions()) {
                    if (indexExpr == null) return null;
                    TypeOrExpressionThereof indexType = new TypeOrExpressionThereof(indexExpr);
                    parameters.add(new Parameter(null, indexType));
                }

                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(accessExpr);
                return new CreateMethodFromUsageFix(accessExpr, arrayType, "get", returnType, parameters);
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateSetMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetArrayAccessExpression accessExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetArrayAccessExpression.class);
                if (accessExpr == null) return null;
                JetExpression arrayExpr = accessExpr.getArrayExpression();
                TypeOrExpressionThereof arrayType = new TypeOrExpressionThereof(arrayExpr);

                List<Parameter> parameters = new ArrayList<Parameter>();
                for (JetExpression indexExpr : accessExpr.getIndexExpressions()) {
                    if (indexExpr == null) return null;
                    TypeOrExpressionThereof indexType = new TypeOrExpressionThereof(indexExpr);
                    parameters.add(new Parameter(null, indexType));
                }

                JetBinaryExpression assignmentExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetBinaryExpression.class);
                if (assignmentExpr == null) return null;
                JetExpression rhs = assignmentExpr.getRight();
                if (rhs == null) return null;
                TypeOrExpressionThereof valType = new TypeOrExpressionThereof(rhs);
                parameters.add(new Parameter("value", valType));

                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getUnitType());
                return new CreateMethodFromUsageFix(accessExpr, arrayType, "set", returnType, parameters);
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateHasNextMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                return null; // TODO
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateNextMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                return null; // TODO
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateIteratorMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                return null; // TODO
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateComponentMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                return null; // TODO
            }
        };
    }
}
