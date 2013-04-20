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
import com.intellij.openapi.fileEditor.FileEditorManager;
import com.intellij.openapi.progress.ProcessCanceledException;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.PopupChooserBuilder;
import com.intellij.openapi.util.TextRange;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.psi.PsiDocumentManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiReference;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.refactoring.psi.SearchUtils;
import com.intellij.ui.components.JBList;
import com.intellij.util.ArrayUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NonNls;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.*;
import org.jetbrains.jet.lang.descriptors.impl.MutableClassDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.diagnostics.DiagnosticWithParameters1;
import org.jetbrains.jet.lang.diagnostics.Errors;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.resolve.BindingContextUtils;
import org.jetbrains.jet.lang.resolve.DescriptorUtils;
import org.jetbrains.jet.lang.resolve.name.Name;
import org.jetbrains.jet.lang.resolve.scopes.JetScope;
import org.jetbrains.jet.lang.types.*;
import org.jetbrains.jet.lang.types.checker.JetTypeChecker;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.codeInsight.ReferenceToClassesShortening;
import org.jetbrains.jet.plugin.presentation.JetLightClassListCellRenderer;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;
import org.jetbrains.jet.plugin.refactoring.JetNameSuggester;
import org.jetbrains.jet.plugin.refactoring.JetNameValidator;
import org.jetbrains.jet.plugin.references.JetSimpleNameReference;
import org.jetbrains.jet.renderer.DescriptorRenderer;

import javax.swing.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CreateMethodFromUsageFix extends CreateFromUsageFixBase {
    private static final String TYPE_PARAMETER_LIST_VARIABLE_NAME = "typeParameterList";
    private static final String TEMPLATE_FROM_USAGE_METHOD_BODY = "New Kotlin Method Body.kt";
    private static final Pattern COMPONENT_FUNCTION_PATTERN = Pattern.compile("^component(\\d+)$");

    /**
     * Represents a concrete type or a set of types yet to be inferred from an expression.
     */
    private static class TypeOrExpressionThereof {
        private final JetExpression expressionOfType;
        private final JetType type;
        private final Variance variance;
        private JetType[] cachedTypeCandidates;
        private String[] cachedNameCandidatesFromExpression;

        public TypeOrExpressionThereof(@NotNull JetExpression expressionOfType) {
            this(expressionOfType, Variance.IN_VARIANCE);
        }

        public TypeOrExpressionThereof(@NotNull JetExpression expressionOfType, Variance variance) {
            this(expressionOfType, null, variance);
        }

        public TypeOrExpressionThereof(@NotNull JetType type, Variance variance) {
            this(null, type, variance);
        }

        private TypeOrExpressionThereof(@Nullable JetExpression expressionOfType, @Nullable JetType type, Variance variance) {
            this.expressionOfType = expressionOfType;
            this.type = type;
            this.variance = variance;
        }

        public boolean isType() {
            return this.type != null;
        }

        @NotNull
        public JetType getType() {
            assert this.type != null;
            return this.type;
        }

        /**
         * Returns a collection containing the possible types represented by this instance. Infers the type from an expression if necessary.
         * @return A collection containing the possible types represented by this instance.
         */
        @NotNull
        public JetType[] getPossibleTypes(@Nullable("use cached, don't recompute") BindingContext context) {
            if (context == null) {
                assert cachedTypeCandidates != null;
                return cachedTypeCandidates;
            }
            List<JetType> types = new ArrayList<JetType>();
            if (isType()) {
                assert type != null : "!isType() means type == null && expressionOfType != null";
                types.add(type);
                types.addAll(TypeUtils.getAllSupertypes(type));
            } else {
                assert expressionOfType != null : "!isType() means type == null && expressionOfType != null";
                for (JetType type : guessTypeForExpression(expressionOfType, context)) {
                    types.add(type);
                    if (variance == Variance.IN_VARIANCE) {
                        types.addAll(TypeUtils.getAllSupertypes(type));
                    }
                }
            }

            if (types.isEmpty()) {
                types.add(KotlinBuiltIns.getInstance().getAnyType());
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
            if (isType()) {
                cachedNameCandidatesFromExpression = ArrayUtil.EMPTY_STRING_ARRAY;
            } else {
                assert expressionOfType != null : "!isType() means type == null && expressionOfType != null";
                JetNameValidator dummyValidator = JetNameValidator.getEmptyValidator(expressionOfType.getProject());
                cachedNameCandidatesFromExpression = JetNameSuggester.suggestNamesForExpression(expressionOfType, dummyValidator);
            }
            return cachedNameCandidatesFromExpression;
        }

        public void substitute(TypeSubstitution[] substitutions) {
            cachedTypeCandidates = substituteTypes(cachedTypeCandidates, substitutions, variance);
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
        private final TypeParameterDescriptor[] typeParametersFromReceiverType;
        private final Map<String, TypeParameterDescriptor[]> typeParameterMap;

        public TypeParameterListExpression(@NotNull TypeParameterDescriptor[] typeParametersFromReceiverType,
                @NotNull Map<String, TypeParameterDescriptor[]> typeParametersMap) {
            this.typeParametersFromReceiverType = typeParametersFromReceiverType;
            this.typeParameterMap = typeParametersMap;
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

            Set<TypeParameterDescriptor> typeParameters = new LinkedHashSet<TypeParameterDescriptor>();
            Collections.addAll(typeParameters, typeParametersFromReceiverType);
            for (JetParameter parameter : parameters) {
                JetTypeReference parameterTypeRef = parameter.getTypeReference();
                assert parameterTypeRef != null;
                TypeParameterDescriptor[] typeParametersFromParameter = typeParameterMap.get(parameterTypeRef.getText());
                if (typeParametersFromParameter != null) {
                    Collections.addAll(typeParameters, typeParametersFromParameter);
                }
            }
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            if (returnTypeRef != null) {
                TypeParameterDescriptor[] typeParametersFromReturnType = typeParameterMap.get(returnTypeRef.getText());
                if (typeParametersFromReturnType != null) {
                    Collections.addAll(typeParameters, typeParametersFromReturnType);
                }
            }

            List<String> typeParameterNames = new ArrayList<String>();
            for (TypeParameterDescriptor typeParameter : typeParameters) {
                typeParameterNames.add(typeParameter.getName().getIdentifier());
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
    private Editor currentFileEditor;
    private Editor containingFileEditor;
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
    public void invoke(@NotNull final Project project, Editor editor, PsiFile file) throws IncorrectOperationException {
        assert file != null && file instanceof JetFile; // TODO: change some assertions to notifications
        currentFile = (JetFile) file;
        currentFileEditor = editor;

        JetType[] possibleOwnerTypes = ownerType.getPossibleTypes();
        assert possibleOwnerTypes.length > 0;
        if (possibleOwnerTypes.length == 1) {
            JetType ownerType = possibleOwnerTypes[0];
            doInvoke(project, ownerType);
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
                            doInvoke(project, ownerType);
                        }
                    }, getText(), null);
                }
            };

            builder.setTitle(JetBundle.message("choose.target.class.or.trait.title"))
                   .setItemChoosenCallback(runnable)
                   .createPopup()
                   .showInBestPositionFor(currentFileEditor);
        }
    }

    private void doInvoke(@NotNull final Project project, @NotNull JetType ownerType) {
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
                buildAndRunTemplate(project, func);
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
            containingFileEditor = currentFileEditor;
            func = (JetNamedFunction) currentFile.add(func);
        } else { // create as method
            String methodText = String.format("fun %s(%s)%s { }", methodName, parametersString, returnTypeString);
            func = JetPsiFactory.createFunction(project, methodText);
            PsiFile classContainingFile = ownerClass.getContainingFile();
            assert classContainingFile instanceof JetFile;
            containingFile = (JetFile) classContainingFile;

            VirtualFile virtualFile = containingFile.getVirtualFile();
            assert virtualFile != null;
            FileEditorManager fileEditorManager = FileEditorManager.getInstance(project);
            fileEditorManager.openFile(virtualFile, true);
            containingFileEditor = fileEditorManager.getSelectedTextEditor();

            JetClassBody classBody = ownerClass.getBody();
            assert classBody != null;
            PsiElement rBrace = classBody.getRBrace();
            assert rBrace != null;
            func = (JetNamedFunction) classBody.addBefore(func, rBrace);
        }

        return func;
    }

    private void buildAndRunTemplate(@NotNull final Project project, @NotNull JetNamedFunction func) {
        JetParameterList parameterList = func.getValueParameterList();
        assert parameterList != null;

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
        PsiDocumentManager.getInstance(project).doPostponedOperationsAndUnblockDocument(containingFileEditor.getDocument());

        CaretModel caretModel = containingFileEditor.getCaretModel();
        caretModel.moveToOffset(containingFile.getNode().getStartOffset());

        TemplateBuilderImpl builder = new TemplateBuilderImpl(containingFile);
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
        TemplateManager.getInstance(project).startTemplate(containingFileEditor, template, new TemplateEditingAdapter() {
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
        Map<String, TypeParameterDescriptor[]> typeParameterMap = new HashMap<String, TypeParameterDescriptor[]>();
        Set<TypeParameterDescriptor> receiverTypeParameters = getTypeParametersInType(receiverType);
        TypeParameterDescriptor[] receiverTypeParametersNotInScope = getTypeParameterNamesNotInScope(receiverTypeParameters, scope);
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
        return new TypeParameterListExpression(receiverTypeParametersNotInScope, typeParameterMap);
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
    private static JetType substituteType(@NotNull JetType type, @NotNull TypeSubstitution substitution, @NotNull Variance variance) {
        switch (variance) {
            case INVARIANT:
                // for invariant, can replace only when they're equal
                if (type.equals(substitution.getForType())) {
                    return substitution.getByType();
                }
                break;
            case IN_VARIANCE:
                // for covariant (e.g. function parameter), can replace type with any of its supertypes
                if (JetTypeChecker.INSTANCE.isSubtypeOf(type, substitution.getForType())) {
                    return substitution.getByType();
                }
                break;
            case OUT_VARIANCE:
                // for contravariant (e.g. function return value), can replace type with any of its subtypes
                if (JetTypeChecker.INSTANCE.isSubtypeOf(substitution.getForType(), type)) {
                    return substitution.getByType();
                }
                break;
        }

        List<TypeProjection> newArguments = new ArrayList<TypeProjection>();
        List<TypeParameterDescriptor> typeParameters = type.getConstructor().getParameters();
        int i = 0;
        for (TypeProjection projection : type.getArguments()) {
            TypeParameterDescriptor typeParameter = typeParameters.get(i);
            JetType newArgument = substituteType(projection.getType(), substitution, typeParameter.getVariance());
            newArguments.add(new TypeProjection(Variance.INVARIANT, newArgument));
            i++;
        }
        return new JetTypeImpl(type.getAnnotations(), type.getConstructor(),
                               type.isNullable(), newArguments, type.getMemberScope());
    }

    @NotNull
    private static JetType[] substituteTypes(@NotNull JetType[] types, @NotNull TypeSubstitution[] substitutions, @NotNull Variance variance) {
        Set<JetType> newTypes = new LinkedHashSet<JetType>(Arrays.asList(types));
        for (TypeSubstitution substitution : substitutions) { // each substitution can be applied or not, so we offer all options
            List<JetType> toAdd = new ArrayList<JetType>();
            List<JetType> toRemove = new ArrayList<JetType>();
            for (JetType type : newTypes) {
                toAdd.add(substituteType(type, substitution, variance));
                // substitution.byType are type arguments, but they cannot already occur before substitution
                if (containsType(type, substitution.getByType())) {
                    toRemove.add(type);
                }
            }
            newTypes.addAll(toAdd);
            newTypes.removeAll(toRemove);
        }
        return newTypes.toArray(new JetType[newTypes.size()]);
    }

    private static boolean containsType(JetType outer, JetType inner) {
        if (outer.equals(inner)) {
            return true;
        }

        for (TypeProjection projection : outer.getArguments()) {
            if (containsType(projection.getType(), inner)) {
                return true;
            }
        }

        return false;
    }

    @NotNull
    private static String renderTypeShort(@NotNull JetType type) {
        return DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(type);
    }

    @NotNull
    private static String renderTypeLong(@NotNull JetType type) {
        return DescriptorRenderer.TEXT.renderType(type);
    }

    @NotNull
    private static TypeParameterDescriptor[] getTypeParameterNamesNotInScope(
            @NotNull Collection<? extends TypeParameterDescriptor> typeParameters,
            @NotNull JetScope scope
    ) {
        List<TypeParameterDescriptor> typeParameterNames = new ArrayList<TypeParameterDescriptor>();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            ClassifierDescriptor classifier = scope.getClassifier(typeParameter.getName());
            if (classifier == null || !classifier.equals(typeParameter)) {
                typeParameterNames.add(typeParameter);
            }
        }
        return typeParameterNames.toArray(new TypeParameterDescriptor[typeParameterNames.size()]);
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
        JetType type = context.get(BindingContext.EXPRESSION_TYPE, expr);
        JetNamedDeclaration declaration = null;

        // if we know the actual type of the expression
        if (type != null) {
            return new JetType[] {type};
        }

        // expression has an expected type
        else if ((type = context.get(BindingContext.EXPECTED_EXPRESSION_TYPE, expr)) != null) {
            return new JetType[] {type};
        }

        // expression itself is a type assertion
        else if (expr instanceof JetTypeConstraint) { // expression itself is a type assertion
            JetTypeConstraint constraint = (JetTypeConstraint) expr;
            return new JetType[] {context.get(BindingContext.TYPE, constraint.getBoundTypeReference())};
        }

        // expression is on the left side of a type assertion
        else if (expr.getParent() instanceof JetTypeConstraint) {
            JetTypeConstraint constraint = (JetTypeConstraint) expr.getParent();
            return new JetType[] {context.get(BindingContext.TYPE, constraint.getBoundTypeReference())};
        }

        // expression is on the lhs of a multi-declaration
        else if (expr instanceof JetMultiDeclarationEntry) {
            JetMultiDeclarationEntry entry = (JetMultiDeclarationEntry) expr;
            JetTypeReference typeRef = entry.getTypeRef();
            if (typeRef != null) { // and has a specified type
                return new JetType[] {context.get(BindingContext.TYPE, typeRef)};
            }
            declaration = entry; // otherwise fall through and guess
        }

        // expression is a parameter (e.g. declared in a for-loop)
        else if (expr instanceof JetParameter) {
            JetParameter parameter = (JetParameter) expr;
            JetTypeReference typeRef = parameter.getTypeReference();
            if (typeRef != null) { // and has a specified type
                return new JetType[] {context.get(BindingContext.TYPE, typeRef)};
            }
            declaration = parameter; // otherwise fall through and guess
        }

        // the expression is the RHS of a variable assignment with a specified type
        else if (expr.getParent() instanceof JetVariableDeclaration) {
            JetVariableDeclaration variable = (JetVariableDeclaration) expr.getParent();
            JetTypeReference typeRef = variable.getTypeRef();
            if (typeRef != null) { // and has a specified type
                return new JetType[] {context.get(BindingContext.TYPE, typeRef)};
            }
            declaration = variable; // otherwise fall through and guess, based on LHS
        }

        // guess based on declaration
        SearchScope scope = expr.getContainingFile().getUseScope();
        Set<JetType> expectedTypes = new HashSet<JetType>();
        if (declaration != null) {
            for (PsiReference ref : SearchUtils.findAllReferences(declaration, scope)) {
                if (ref instanceof JetSimpleNameReference) {
                    JetSimpleNameReference simpleNameRef = (JetSimpleNameReference) ref;
                    JetType expectedType = context.get(BindingContext.EXPECTED_EXPRESSION_TYPE, simpleNameRef.getExpression());
                    if (expectedType != null) {
                        expectedTypes.add(expectedType);
                    }
                }
            }
        }
        if (expectedTypes.isEmpty()) {
            return new JetType[0];
        }
        type = TypeUtils.intersect(JetTypeChecker.INSTANCE, expectedTypes);
        if (type != null) {
            return new JetType[] {type};
        } else { // intersection doesn't exist; let user make an imperfect choice
            return expectedTypes.toArray(new JetType[expectedTypes.size()]);
        }
    }

    private static boolean isUnit(@NotNull JetType type) {
        return KotlinBuiltIns.getInstance().isUnit(type);
    }

    private static void assertNoUnitTypes(@NotNull JetType[] types) {
        for (JetType type : types) {
            assert !isUnit(type);
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
                if (arrayExpr == null) return null;
                TypeOrExpressionThereof arrayType = new TypeOrExpressionThereof(arrayExpr);

                List<Parameter> parameters = new ArrayList<Parameter>();
                for (JetExpression indexExpr : accessExpr.getIndexExpressions()) {
                    if (indexExpr == null) return null;
                    TypeOrExpressionThereof indexType = new TypeOrExpressionThereof(indexExpr);
                    parameters.add(new Parameter(null, indexType));
                }

                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(accessExpr, Variance.OUT_VARIANCE);
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
                if (arrayExpr == null) return null;
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

                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getUnitType(), Variance.OUT_VARIANCE);
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
                JetForExpression forExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetForExpression.class);
                if (forExpr == null) return null;
                JetExpression iterableExpr = forExpr.getLoopRange();
                if (iterableExpr == null) return null;
                TypeOrExpressionThereof iterableType = new TypeOrExpressionThereof(iterableExpr);
                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(KotlinBuiltIns.getInstance().getBooleanType(), Variance.OUT_VARIANCE);
                return new CreateMethodFromUsageFix(forExpr, iterableType, "hasNext", returnType, new ArrayList<Parameter>());
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateNextMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetForExpression forExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetForExpression.class);
                if (forExpr == null) return null;
                JetExpression iterableExpr = forExpr.getLoopRange();
                if (iterableExpr == null) return null;
                JetExpression variableExpr = forExpr.getLoopParameter();
                if (variableExpr == null) return null;
                TypeOrExpressionThereof iterableType = new TypeOrExpressionThereof(iterableExpr);
                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(variableExpr, Variance.OUT_VARIANCE);
                return new CreateMethodFromUsageFix(forExpr, iterableType, "next", returnType, new ArrayList<Parameter>());
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateIteratorMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                PsiFile file = diagnostic.getPsiFile();
                if (!(file instanceof JetFile)) return null;
                JetFile jetFile = (JetFile) file;

                JetForExpression forExpr = QuickFixUtil.getParentElementOfType(diagnostic, JetForExpression.class);
                if (forExpr == null) return null;
                JetExpression iterableExpr = forExpr.getLoopRange();
                if (iterableExpr == null) return null;
                JetExpression variableExpr = forExpr.getLoopParameter();
                if (variableExpr == null) return null;
                TypeOrExpressionThereof iterableType = new TypeOrExpressionThereof(iterableExpr);
                JetType returnJetType = KotlinBuiltIns.getInstance().getIterator().getDefaultType();

                BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache(jetFile).getBindingContext();
                JetType[] returnJetTypeParameterTypes = guessTypeForExpression(variableExpr, context);
                if (returnJetTypeParameterTypes.length != 1) return null;

                TypeProjection returnJetTypeParameterType = new TypeProjection(returnJetTypeParameterTypes[0]);
                List<TypeProjection> returnJetTypeArguments = Collections.singletonList(returnJetTypeParameterType);
                returnJetType = new JetTypeImpl(returnJetType.getAnnotations(), returnJetType.getConstructor(), returnJetType.isNullable(),
                                                returnJetTypeArguments, returnJetType.getMemberScope());
                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(returnJetType, Variance.OUT_VARIANCE);
                return new CreateMethodFromUsageFix(forExpr, iterableType, "iterator", returnType, new ArrayList<Parameter>());
            }
        };
    }

    @NotNull
    public static JetIntentionActionFactory createCreateComponentMethodFromUsageFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetMultiDeclaration multiDeclaration = QuickFixUtil.getParentElementOfType(diagnostic, JetMultiDeclaration.class);
                if (multiDeclaration == null) return null;
                List<JetMultiDeclarationEntry> entries = multiDeclaration.getEntries();

                assert diagnostic.getFactory() == Errors.COMPONENT_FUNCTION_MISSING;
                @SuppressWarnings("unchecked")
                DiagnosticWithParameters1<JetExpression, Name> diagnosticWithParameters =
                        (DiagnosticWithParameters1<JetExpression, Name>) diagnostic;
                Name name = diagnosticWithParameters.getA();
                Matcher componentNumberMatcher = COMPONENT_FUNCTION_PATTERN.matcher(name.getIdentifier());
                if (!componentNumberMatcher.matches()) return null;
                String componentNumberString = componentNumberMatcher.group(1);
                int componentNumber = Integer.decode(componentNumberString) - 1;

                JetMultiDeclarationEntry entry = entries.get(componentNumber);
                TypeOrExpressionThereof returnType = new TypeOrExpressionThereof(entry, Variance.OUT_VARIANCE);
                JetExpression rhs = multiDeclaration.getInitializer();
                if (rhs == null) return null;
                TypeOrExpressionThereof ownerType = new TypeOrExpressionThereof(rhs);

                return new CreateMethodFromUsageFix(multiDeclaration, ownerType, name.getIdentifier(), returnType, new ArrayList<Parameter>());
            }
        };
    }
}
