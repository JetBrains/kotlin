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
import com.intellij.ide.util.PsiElementListCellRenderer;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.command.CommandProcessor;
import com.intellij.openapi.editor.CaretModel;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.ui.components.JBList;
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
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.presentation.JetLightClassListCellRenderer;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.util.*;

public class CreateMethodFromUsageFix extends CreateFromUsageFixBase {
    private static final String TYPE_PARAMETER_LIST_VARIABLE_NAME = "typeParameterList";
    private static final String TEMPLATE_FROM_USAGE_METHOD_BODY = "New Kotlin Method Body.kt";

    /**
     * Represents a concrete type or a set of types yet to be inferred from an expression.
     */
    private static class TypeOrExpressionThereof {
        private final JetExpression expressionOfType;
        private JetType type;
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

        public void substitute(TypeSubstitution[] substitutions) {
            if (type != null) {
                for (TypeSubstitution substitution : substitutions) {
                    type = substituteType(type, substitution);
                }
            }
            cachedTypeCandidates = substituteTypes(cachedTypeCandidates, substitutions);
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
            return calculateResult(context);
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
        private final LookupElement[] cachedLookupElements;

        public TypeExpression(@NotNull JetType[] options) {
            this.options = options;
            optionStrings = new String[options.length];
            cachedLookupElements = new LookupElement[options.length];
            for (int i = 0; i < options.length; i++) {
                optionStrings[i] = renderTypeShort(options[i]);
                cachedLookupElements[i] = LookupElementBuilder.create(options[i], optionStrings[i]);
            }
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
            return calculateResult(context);
        }

        @NotNull
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            return cachedLookupElements;
        }

        @NotNull
        public JetType[] getOptions() {
            return options;
        }

        @NotNull
        public String[] getOptionStrings() {
            return optionStrings;
        }

        @Nullable("can't be found")
        public JetType getTypeFromSelection(@NotNull String selection) {
            for (int i = 0; i < options.length; i++) {
                if (optionStrings[i].equals(selection)) {
                    return options[i];
                }
            }
            return null;
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

            // make sure there are no name conflicts
            for (int i = 0; i < typeParameterNames.size(); i++) {
                String name = typeParameterNames.get(i);
                name = getNextAvailableName(name, typeParameterNames.subList(0, i));
                typeParameterNames.set(i, name);
            }

            return typeParameterNames.isEmpty()
                    ? new TextResult("")
                    : new TextResult(" <" + StringUtil.join(typeParameterNames, ", ") + ">");
        }

        @Nullable
        @Override
        public Result calculateQuickResult(ExpressionContext context) {
            return calculateResult(context);
        }

        @NotNull
        @Override
        public LookupElement[] calculateLookupItems(ExpressionContext context) {
            return new LookupElement[0]; // do not offer the user any choices
        }
    }

    /**
     * Encapsulates a single type substitution of a <code>JetType</code> by another <code>JetType</code>.
     */
    private static class TypeSubstitution {
        private final JetType forType;
        private final JetType byType;

        private TypeSubstitution(JetType forType, JetType byType) {
            this.forType = forType;
            this.byType = byType;
        }

        private JetType getForType() {
            return forType;
        }

        private JetType getByType() {
            return byType;
        }
    }

    private final String methodName;
    private final TypeOrExpressionThereof ownerType;
    private final TypeOrExpressionThereof returnType;
    private final List<Parameter> parameters;

    private boolean isUnit;
    private boolean isExtension;
    private JetFile currentFile;
    private JetFile containingFile;
    private BindingContext currentFileContext;
    private JetClass ownerClass;
    private ClassDescriptor ownerClassDescriptor;
    private JetType receiverType;

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
    public void invoke(@NotNull final Project project, final Editor editor, PsiFile file) throws IncorrectOperationException {
        // TODO: editing across files

        assert file instanceof JetFile;
        currentFile = (JetFile) file;

        JetType[] possibleOwnerTypes = ownerType.getPossibleTypes(currentFileContext);
        assert possibleOwnerTypes.length > 0;
        if (possibleOwnerTypes.length == 1) {
            JetType ownerType = possibleOwnerTypes[0];
            doInvoke(project, editor, ownerType);
        } else {
            // class selection
            List<String> options = new ArrayList<String>();
            final Map<String, JetType> optionToTypeMap = new HashMap<String, JetType>();
            for (JetType possibleOwnerType : possibleOwnerTypes) {
                ClassifierDescriptor possibleClassDescriptor = possibleOwnerType.getConstructor().getDeclarationDescriptor();
                if (possibleClassDescriptor != null) {
                    String className = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(possibleClassDescriptor.getDefaultType());
                    DeclarationDescriptor namespaceDescriptor = possibleClassDescriptor.getContainingDeclaration();
                    assert namespaceDescriptor instanceof NamespaceDescriptor;
                    String namespace = ((NamespaceDescriptor) namespaceDescriptor).getFqName().getFqName();
                    String option = className + " (" + namespace + ")";
                    options.add(option);
                    optionToTypeMap.put(option, possibleOwnerType);
                }
            }

            final JList list = new JBList(options);
            PsiElementListCellRenderer renderer = new JetLightClassListCellRenderer();
            list.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
            list.setCellRenderer(renderer);
            PopupChooserBuilder builder = new PopupChooserBuilder(list);
            renderer.installSpeedSearch(builder);

            Runnable runnable = new Runnable() {
                @Override
                public void run() {
                    int index = list.getSelectedIndex();
                    if (index < 0) return;
                    String option = (String) list.getSelectedValue();
                    final JetType ownerType = optionToTypeMap.get(option);
                    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                        @Override
                        public void run() {
                            doInvoke(project, editor, ownerType);
                        }
                    }, getText(), null);
                }
            };

            builder.setTitle(JetBundle.message("choose.target.class.or.trait.title"))
                   .setItemChoosenCallback(runnable)
                   .createPopup()
                   .showInBestPositionFor(editor);
        }
    }

    private void doInvoke(@NotNull final Project project, @NotNull final Editor editor, @NotNull JetType ownerType) {
        // gather relevant information
        ClassifierDescriptor ownerTypeDescriptor = ownerType.getConstructor().getDeclarationDescriptor();
        assert ownerTypeDescriptor != null && ownerTypeDescriptor instanceof ClassDescriptor;
        ownerClassDescriptor = (ClassDescriptor) ownerTypeDescriptor;
        receiverType = ownerClassDescriptor.getDefaultType();
        PsiElement typeDeclaration = BindingContextUtils.classDescriptorToDeclaration(currentFileContext, ownerClassDescriptor);
        if (typeDeclaration != null && typeDeclaration instanceof JetClass) {
            ownerClass = (JetClass) typeDeclaration;
            isExtension = !ownerClass.isWritable();
        } else {
            isExtension = true;
        }
        isUnit = returnType.isType() && isUnit(returnType.getType());

        // figure out type substitutions for type parameters
        List<TypeProjection> classTypeParameters = receiverType.getArguments();
        List<TypeProjection> ownerTypeArguments = ownerType.getArguments();
        assert ownerTypeArguments.size() == classTypeParameters.size();
        TypeSubstitution[] substitutions = new TypeSubstitution[classTypeParameters.size()];
        for (int i = 0; i < substitutions.length; i++) {
            substitutions[i] = new TypeSubstitution(ownerTypeArguments.get(i).getType(), classTypeParameters.get(i).getType());
        }
        returnType.substitute(substitutions);
        for (Parameter parameter : parameters) {
            parameter.getType().substitute(substitutions);
        }

        ApplicationManager.getApplication().runWriteAction(new Runnable() {
            @Override
            public void run() {
                JetNamedFunction func = createFunctionSkeleton(project);
                buildAndRunTemplate(project, editor, func);
            }
        });
    }

    private JetNamedFunction createFunctionSkeleton(@NotNull Project project) {
        JetNamedFunction func;
        String[] parameterStrings = new String[parameters.size()];
        for (int i = 0; i < parameterStrings.length; i++) {
            parameterStrings[i] = "p" + i + ": Any";
        }
        String parametersString = StringUtil.join(parameterStrings,", ");
        String returnTypeString = isUnit ? "" : ": Any";
        if (isExtension) { // create as extension function
            String ownerTypeString = renderTypeShort(receiverType);
            String methodText = String.format("fun %s.%s(%s)%s { }", ownerTypeString, methodName, parametersString, returnTypeString);
            func = JetPsiFactory.createFunction(project, methodText);
            containingFile = currentFile;
            func = (JetNamedFunction) currentFile.add(func);
        } else { // create as method
            String methodText = String.format("fun %s(%s)%s { }", methodName, parametersString, returnTypeString);
            func = JetPsiFactory.createFunction(project, methodText);
            PsiFile classContainingFile = ownerClass.getContainingFile();
            assert classContainingFile instanceof JetFile;
            containingFile = (JetFile) classContainingFile;
            JetClassBody classBody = ownerClass.getBody();
            assert classBody != null;
            PsiElement rBrace = classBody.getRBrace();
            func = (JetNamedFunction) classBody.addBefore(func, rBrace);
        }
        // TODO: add newlines
        return func;
    }

    private void buildAndRunTemplate(@NotNull final Project project, @NotNull Editor editor, @NotNull JetNamedFunction func) {
        JetParameterList parameterList = func.getValueParameterList();
        assert parameterList != null;

        BindingContext containingFileContext = currentFile.equals(containingFile)
                                               ? currentFileContext
                                               : AnalyzerFacadeWithCache.analyzeFileWithCache(containingFile).getBindingContext();
        JetScope scope;
        if (isExtension) {
            NamespaceDescriptor namespaceDescriptor = currentFileContext.get(BindingContext.FILE_TO_NAMESPACE, containingFile);
            assert namespaceDescriptor != null;
            scope = namespaceDescriptor.getMemberScope();
        } else {
            assert ownerClassDescriptor instanceof MutableClassDescriptor;
            scope = ((MutableClassDescriptor) ownerClassDescriptor).getScopeForMemberResolution();
        }

        // build templates
        PsiDocumentManager.getInstance(project).commitAllDocuments();
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(editor.getDocument());

        final CaretModel caretModel = editor.getCaretModel();
        final int oldOffset = caretModel.getOffset();
        caretModel.moveToOffset(currentFile.getNode().getStartOffset());

        TemplateBuilderImpl builder = new TemplateBuilderImpl(currentFile);
        final TypeExpression returnTypeExpression = isUnit ? null : setupReturnTypeTemplate(builder, func);
        final TypeExpression[] parameterTypeExpressions = setupParameterTypeTemplates(project, builder, parameterList);

        // add a segment for the parameter list
        // Note: because TemplateBuilderImpl does not have a replaceElement overload that takes in both a TextRange and alwaysStopAt, we
        // need to create the segment first and then hack the Expression into the template later. We use this template to update the type
        // parameter list as the user makes selections in the parameter types, and we need alwaysStopAt to be false so the user can't tab to
        // it.
        TypeParameterListExpression expression =
                setupTypeParameterListTemplate(builder, func, parameterTypeExpressions, returnTypeExpression, scope);

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
                // file templates
                int offset = template.getSegmentOffset(0);
                final JetNamedFunction func = PsiTreeUtil.findElementOfClassAtOffset(containingFile, offset, JetNamedFunction.class, false);
                assert func != null;
                final List<JetTypeReference> typeRefsToShorten = new ArrayList<JetTypeReference>();

                ApplicationManager.getApplication().runWriteAction(new Runnable() {
                    @Override
                    public void run() {
                        // file templates
                        setupFunctionBody(project, func);

                        // change short type names to fully qualified ones (to be shortened below)
                        setupTypeReferencesForShortening(project, func, typeRefsToShorten, parameterTypeExpressions, returnTypeExpression);
                    }
                });

                ReferenceToClassesShortening.compactReferenceToClasses(typeRefsToShorten);

                caretModel.moveToOffset(oldOffset);
            }
        });
    }

    private void setupTypeReferencesForShortening(
            @NotNull Project project,
            @NotNull JetNamedFunction func,
            @NotNull List<JetTypeReference> typeRefsToShorten,
            @NotNull TypeExpression[] parameterTypeExpressions,
            @Nullable TypeExpression returnTypeExpression
    ) {
        if (isExtension) {
            JetTypeReference receiverTypeRef = JetPsiFactory.createType(project, renderTypeLong(receiverType));
            replaceWithLongerName(project, receiverTypeRef, receiverType);

            receiverTypeRef = func.getReceiverTypeRef();
            assert receiverTypeRef != null;
            typeRefsToShorten.add(receiverTypeRef);
        }

        if (!isUnit) {
            assert returnTypeExpression != null;
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            assert returnTypeRef != null;
            JetType returnType = returnTypeExpression.getTypeFromSelection(returnTypeRef.getText());
            if (returnType != null) { // user selected a given type
                replaceWithLongerName(project, returnTypeRef, returnType);
                returnTypeRef = func.getReturnTypeRef();
                assert returnTypeRef != null;
                typeRefsToShorten.add(returnTypeRef);
            }
        }

        List<JetParameter> valueParameters = func.getValueParameters();
        List<Integer> parameterIndicesToShorten = new ArrayList<Integer>();
        assert valueParameters.size() == parameterTypeExpressions.length;
        for (int i = 0; i < valueParameters.size(); i++) {
            JetParameter parameter = valueParameters.get(i);
            JetTypeReference parameterTypeRef = parameter.getTypeReference();
            assert parameterTypeRef != null;
            JetType parameterType = parameterTypeExpressions[i].getTypeFromSelection(parameterTypeRef.getText());
            if (parameterType != null) {
                replaceWithLongerName(project, parameterTypeRef, parameterType);
                parameterIndicesToShorten.add(i);
            }
        }
        valueParameters = func.getValueParameters();
        for (int i : parameterIndicesToShorten) {
            JetTypeReference parameterTypeRef = valueParameters.get(i).getTypeReference();
            assert parameterTypeRef != null;
            typeRefsToShorten.add(parameterTypeRef);
        }
    }

    private void setupFunctionBody(@NotNull Project project, @NotNull JetNamedFunction func) {
        FileTemplate fileTemplate = FileTemplateManager.getInstance().getCodeTemplate(TEMPLATE_FROM_USAGE_METHOD_BODY);
        Properties properties = new Properties();
        if (isUnit) {
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, "Unit");
        } else {
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            assert returnTypeRef != null;
            properties.setProperty(FileTemplate.ATTRIBUTE_RETURN_TYPE, returnTypeRef.getText());
        }
        properties.setProperty(FileTemplate.ATTRIBUTE_CLASS_NAME, DescriptorUtils.getFQName(ownerClassDescriptor).getFqName());
        properties.setProperty(FileTemplate.ATTRIBUTE_SIMPLE_CLASS_NAME, ownerClassDescriptor.getName().getName());
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
    private TypeExpression setupReturnTypeTemplate(@NotNull TemplateBuilder builder, @NotNull JetNamedFunction func) {
        JetTypeReference returnTypeRef = func.getReturnTypeRef();
        assert returnTypeRef != null;
        TypeExpression returnTypeExpression = new TypeExpression(returnType.getPossibleTypes());
        builder.replaceElement(returnTypeRef, returnTypeExpression);
        return returnTypeExpression;
    }

    @NotNull
    private TypeParameterListExpression setupTypeParameterListTemplate(
            @NotNull TemplateBuilderImpl builder,
            @NotNull JetNamedFunction func,
            @NotNull TypeExpression[] parameterTypeExpressions,
            @Nullable TypeExpression returnTypeExpression,
            @NotNull JetScope scope
    ) {
        Map<String, String[]> typeParameterMap = new HashMap<String, String[]>();
        Set<TypeParameterDescriptor> receiverTypeParameters = getTypeParametersInType(receiverType);
        String[] ownerTypeParameterNames = getTypeParameterNamesNotInScope(receiverTypeParameters, scope);
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

    private TypeExpression[] setupParameterTypeTemplates(@NotNull Project project, @NotNull TemplateBuilder builder,
            @NotNull JetParameterList parameterList) {
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
        currentFileContext = AnalyzerFacadeWithCache.analyzeFileWithCache(jetFile).getBindingContext();

        JetType[] possibleOwnerTypes = ownerType.getPossibleTypes(currentFileContext);
        if (possibleOwnerTypes.length == 0) return false;
        assertNoUnitTypes(possibleOwnerTypes);
        JetType[] possibleReturnTypes = returnType.getPossibleTypes(currentFileContext);
        if (!returnType.isType()) { // allow return type to be unit (but not when it's among several options)
            assertNoUnitTypes(possibleReturnTypes);
        }
        if (possibleReturnTypes.length == 0) return false;
        for (Parameter parameter : parameters) {
            JetType[] possibleTypes = parameter.getType().getPossibleTypes(currentFileContext);
            if (possibleTypes.length == 0) return false;
            assertNoUnitTypes(possibleTypes);
        }

        return true;
    }

    private static void replaceWithLongerName(@NotNull Project project, @NotNull JetTypeReference typeRef, @NotNull JetType type) {
        JetTypeReference fullyQualifiedReceiverTypeRef = JetPsiFactory.createType(project, renderTypeLong(type));
        typeRef.replace(fullyQualifiedReceiverTypeRef);
    }

    @NotNull
    private static JetType substituteType(@NotNull JetType type, @NotNull TypeSubstitution substitution) {
        if (type.equals(substitution.getForType())) {
            return substitution.getByType();
        }

        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        for (TypeProjection projection : type.getArguments()) {
            JetType newArgument = substituteType(projection.getType(), substitution);
            newArguments.add(new TypeProjection(Variance.INVARIANT, newArgument));
        }
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(),
                               type.isNullable(), newArguments, type.getMemberScope());
    }

    @NotNull
    private static JetType[] substituteTypes(@NotNull JetType[] types, @NotNull TypeSubstitution[] substitutions) {
        JetType[] newTypes = new JetType[types.length];
        for (int i = 0; i < types.length; i++) {
            JetType newType = types[i];
            for (TypeSubstitution substitution : substitutions) {
                newType = substituteType(newType, substitution);
            }
            newTypes[i] = newType;
        }
        return newTypes;
    }

    @NotNull
    private static String renderTypeShort(JetType type) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    @NotNull
    private static String renderTypeLong(JetType type) {
        return DescriptorRenderer.TEXT.renderType(type);
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
    private static String getNextAvailableName(@NotNull String name, @NotNull Collection<String> existingNames) {
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
