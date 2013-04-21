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
     * Represents a single choice for a type (e.g. parameter type or return type).
     */
    private static class TypeCandidate {
        private final JetType type;
        private final TypeParameterDescriptor[] typeParameters;
        private String renderedType;
        private String[] typeParameterNames;

        public TypeCandidate(@NotNull JetType type) {
            this.type = type;
            Set<TypeParameterDescriptor> typeParametersInType = getTypeParametersInType(type);
            typeParameters = typeParametersInType.toArray(new TypeParameterDescriptor[typeParametersInType.size()]);
            renderedType = renderTypeShort(type, Collections.<TypeParameterDescriptor, String>emptyMap());
        }

        public TypeCandidate(@NotNull JetType type, @NotNull JetScope scope) {
            this.type = type;
            typeParameters = getTypeParameterNamesNotInScope(getTypeParametersInType(type), scope);
        }

        public void render(@NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap) {
            renderedType = renderTypeShort(type, typeParameterNameMap);
            typeParameterNames = new String[typeParameters.length];
            int i = 0;
            for (TypeParameterDescriptor typeParameter : typeParameters) {
                typeParameterNames[i] = typeParameterNameMap.get(typeParameter);
                i++;
            }
        }

        @NotNull JetType getType() {
            return type;
        }

        @NotNull
        public String getRenderedType() {
            assert renderedType != null : "call render() first";
            return renderedType;
        }

        @NotNull
        public String[] getTypeParameterNames() {
            assert typeParameterNames != null : "call render() first";
            return typeParameterNames;
        }

        @NotNull
        public TypeParameterDescriptor[] getTypeParameters() {
            return typeParameters;
        }
    }

    /**
     * Represents a concrete type or a set of types yet to be inferred from an expression.
     */
    private static class TypeOrExpressionThereof {
        private final JetExpression expressionOfType;
        private final JetType type;
        private final Variance variance;
        private TypeCandidate[] typeCandidates;
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

        @NotNull
        private Collection<JetType> getPossibleTypes(BindingContext context) {
            Collection<JetType> types = new ArrayList<JetType>();
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
            return types;
        }

        public void computeTypeCandidates(@NotNull BindingContext context) {
            Collection<JetType> types = getPossibleTypes(context);

            typeCandidates = new TypeCandidate[types.size()];
            int i = 0;
            for (JetType type : types) {
                typeCandidates[i] = new TypeCandidate(type);
                i++;
            }
        }

        public void computeTypeCandidates(
                @NotNull BindingContext context,
                @NotNull TypeSubstitution[] substitutions,
                @NotNull JetScope scope
        ) {
            Collection<JetType> types = getPossibleTypes(context);

            Set<JetType> newTypes = new LinkedHashSet<JetType>(types);
            for (TypeSubstitution substitution : substitutions) { // each substitution can be applied or not, so we offer all options
                List<JetType> toAdd = new ArrayList<JetType>();
                List<JetType> toRemove = new ArrayList<JetType>();
                for (JetType type : newTypes) {
                    toAdd.add(substituteType(type, substitution, variance));
                    // substitution.byType are type arguments, but they cannot already occur in the type before substitution
                    if (containsType(type, substitution.getByType())) {
                        toRemove.add(type);
                    }
                }
                newTypes.addAll(toAdd);
                newTypes.removeAll(toRemove);
            }

            if (newTypes.isEmpty()) {
                newTypes.add(KotlinBuiltIns.getInstance().getAnyType());
            }

            types = newTypes;

            typeCandidates = new TypeCandidate[types.size()];
            int i = 0;
            for (JetType type : types) {
                typeCandidates[i] = new TypeCandidate(type, scope);
                i++;
            }
        }

        @NotNull
        public TypeCandidate[] getTypeCandidates() {
            assert typeCandidates != null : "call computeTypeCandidates() first";
            return typeCandidates;
        }

        public void renderTypeCandidates(@NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap) {
            for (TypeCandidate candidate : typeCandidates) {
                candidate.render(typeParameterNameMap);
            }
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
            for (String name : names) {
                assert name != null && !name.isEmpty();
            }
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
            if (func == null) return new LookupElement[0];
            JetParameterList parameterList = func.getValueParameterList();
            assert parameterList != null;

            // add names based on selected type
            JetParameter parameter = PsiTreeUtil.getParentOfType(elementAt, JetParameter.class);
            if (parameter != null) {
                JetTypeReference parameterTypeRef = parameter.getTypeReference();
                if (parameterTypeRef != null) {
                    String[] suggestedNamesBasedOnType = parameterTypeToNamesMap.get(parameterTypeRef.getText());
                    if (suggestedNamesBasedOnType != null) {
                        Collections.addAll(names, suggestedNamesBasedOnType);
                    }
                }
            }

            // remember other parameter names for later use
            Set<String> parameterNames = new HashSet<String>();
            for (JetParameter jetParameter : parameterList.getParameters()) {
                if (jetParameter == parameter || jetParameter.getName() == null) continue;
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
        private final TypeOrExpressionThereof type;
        private @NotNull LookupElement[] cachedLookupElements;

        public TypeExpression(@NotNull TypeOrExpressionThereof type) {
            this.type = type;
            TypeCandidate[] candidates = type.getTypeCandidates();
            cachedLookupElements = new LookupElement[candidates.length];
            for (int i = 0; i < candidates.length; i++) {
                cachedLookupElements[i] = LookupElementBuilder.create(candidates[i], candidates[i].getRenderedType());
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
        public TypeOrExpressionThereof getType() {
            return type;
        }

        @Nullable("can't be found")
        public JetType getTypeFromSelection(@NotNull String selection) {
            TypeCandidate[] options = type.getTypeCandidates();
            for (TypeCandidate option : options) {
                if (option.getRenderedType().equals(selection)) {
                    return option.getType();
                }
            }
            return null;
        }
    }

    /**
     * A sort-of dummy <code>Expression</code> for parameter lists, to allow us to update the parameter list as the user makes selections.
     */
    private static class TypeParameterListExpression extends Expression {
        private final String[] typeParameterNamesFromReceiverType;
        private final Map<String, String[]> parameterTypeToTypeParameterNamesMap;

        public TypeParameterListExpression(
                @NotNull String[] typeParameterNamesFromReceiverType,
                @NotNull Map<String, String[]> typeParametersMap
        ) {
            this.typeParameterNamesFromReceiverType = typeParameterNamesFromReceiverType;
            this.parameterTypeToTypeParameterNamesMap = typeParametersMap;
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
            if (func == null) {
                return new TextResult("");
            }
            List<JetParameter> parameters = func.getValueParameters();

            Set<String> typeParameterNames = new LinkedHashSet<String>();
            Collections.addAll(typeParameterNames, typeParameterNamesFromReceiverType);
            for (JetParameter parameter : parameters) {
                JetTypeReference parameterTypeRef = parameter.getTypeReference();
                if (parameterTypeRef != null) {
                    String[] typeParameterNamesFromParameter = parameterTypeToTypeParameterNamesMap.get(parameterTypeRef.getText());
                    if (typeParameterNamesFromParameter != null) {
                        Collections.addAll(typeParameterNames, typeParameterNamesFromParameter);
                    }
                }
            }
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            if (returnTypeRef != null) {
                String[] typeParameterNamesFromReturnType = parameterTypeToTypeParameterNamesMap.get(returnTypeRef.getText());
                if (typeParameterNamesFromReturnType != null) {
                    Collections.addAll(typeParameterNames, typeParameterNamesFromReturnType);
                }
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
    private TypeCandidate selectedReceiverType;
    private Map<TypeParameterDescriptor, String> typeParameterNameMap;

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
        assert file != null && file instanceof JetFile;
        currentFile = (JetFile) file;
        currentFileEditor = editor;
        currentFileContext = AnalyzerFacadeWithCache.analyzeFileWithCache(currentFile).getBindingContext();

        ownerType.computeTypeCandidates(currentFileContext);
        TypeCandidate[] ownerTypeCandidates = ownerType.getTypeCandidates();
        assert ownerTypeCandidates.length > 0;
        if (ownerTypeCandidates.length == 1) {
            selectedReceiverType = ownerTypeCandidates[0];
            doInvoke(project);
        } else {
            // class selection
            List<String> options = new ArrayList<String>();
            final Map<String, TypeCandidate> optionToTypeMap = new HashMap<String, TypeCandidate>();
            for (TypeCandidate ownerTypeCandidate : ownerTypeCandidates) {
                ClassifierDescriptor possibleClassDescriptor = ownerTypeCandidate.getType().getConstructor().getDeclarationDescriptor();
                if (possibleClassDescriptor != null) {
                    String className = DescriptorRenderer.SHORT_NAMES_IN_TYPES.renderType(possibleClassDescriptor.getDefaultType());
                    DeclarationDescriptor namespaceDescriptor = possibleClassDescriptor.getContainingDeclaration();
                    String namespace = renderDescriptor(namespaceDescriptor, true);
                    String option = className + " (" + namespace + ")";
                    options.add(option);
                    optionToTypeMap.put(option, ownerTypeCandidate);
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
                    selectedReceiverType = optionToTypeMap.get(option);
                    CommandProcessor.getInstance().executeCommand(project, new Runnable() {
                        @Override
                        public void run() {
                            doInvoke(project);
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

    private void doInvoke(@NotNull final Project project) {
        // gather relevant information
        ClassifierDescriptor ownerTypeDescriptor = selectedReceiverType.getType().getConstructor().getDeclarationDescriptor();
        assert ownerTypeDescriptor != null && ownerTypeDescriptor instanceof ClassDescriptor;
        ownerClassDescriptor = (ClassDescriptor) ownerTypeDescriptor;
        JetType receiverType = ownerClassDescriptor.getDefaultType();
        PsiElement typeDeclaration = BindingContextUtils.classDescriptorToDeclaration(currentFileContext, ownerClassDescriptor);
        if (typeDeclaration != null && typeDeclaration instanceof JetClass) {
            ownerClass = (JetClass) typeDeclaration;
            isExtension = !ownerClass.isWritable();
        } else {
            isExtension = true;
        }
        isUnit = returnType.isType() && isUnit(returnType.getType());

        JetScope scope;
        if (isExtension) {
            NamespaceDescriptor namespaceDescriptor = currentFileContext.get(BindingContext.FILE_TO_NAMESPACE, containingFile);
            assert namespaceDescriptor != null;
            scope = namespaceDescriptor.getMemberScope();
        } else {
            assert ownerClassDescriptor instanceof MutableClassDescriptor;
            scope = ((MutableClassDescriptor) ownerClassDescriptor).getScopeForMemberResolution();
        }

        // figure out type substitutions for type parameters
        List<TypeProjection> classTypeParameters = receiverType.getArguments();
        List<TypeProjection> ownerTypeArguments = selectedReceiverType.getType().getArguments();
        assert ownerTypeArguments.size() == classTypeParameters.size();
        TypeSubstitution[] substitutions = new TypeSubstitution[classTypeParameters.size()];
        for (int i = 0; i < substitutions.length; i++) {
            substitutions[i] = new TypeSubstitution(ownerTypeArguments.get(i).getType(), classTypeParameters.get(i).getType());
        }
        for (Parameter parameter : parameters) {
            parameter.getType().computeTypeCandidates(currentFileContext, substitutions, scope);
        }
        if (!isUnit) {
            returnType.computeTypeCandidates(currentFileContext, substitutions, scope);
        }

        // now that we have done substitutions, we can throw it away
        selectedReceiverType = new TypeCandidate(receiverType, scope);

        // figure out type parameter renames to avoid conflicts
        typeParameterNameMap = getTypeParameterRenames(scope);
        for (Parameter parameter : parameters) {
            parameter.getType().renderTypeCandidates(typeParameterNameMap);
        }
        if (!isUnit) {
            returnType.renderTypeCandidates(typeParameterNameMap);
        }
        selectedReceiverType.render(typeParameterNameMap);

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
            String ownerTypeString = selectedReceiverType.getRenderedType();
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
            if (classBody == null) {
                classBody = (JetClassBody) ownerClass.add(JetPsiFactory.createEmptyClassBody(project));
                ownerClass.addBefore(JetPsiFactory.createWhiteSpace(project), classBody);
            }
            PsiElement rBrace = classBody.getRBrace();
            assert rBrace != null;
            func = (JetNamedFunction) classBody.addBefore(func, rBrace);
        }

        return func;
    }

    private void buildAndRunTemplate(@NotNull final Project project, @NotNull JetNamedFunction func) {
        JetParameterList parameterList = func.getValueParameterList();
        assert parameterList != null;

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
        TypeParameterListExpression expression = setupTypeParameterListTemplate(builder, func);

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

    private Map<TypeParameterDescriptor, String> getTypeParameterRenames(JetScope scope) {
        TypeParameterDescriptor[] receiverTypeParametersNotInScope = selectedReceiverType.getTypeParameters();
        Set<TypeParameterDescriptor> allTypeParametersNotInScope = new LinkedHashSet<TypeParameterDescriptor>();
        allTypeParametersNotInScope.addAll(Arrays.asList(receiverTypeParametersNotInScope));

        for (Parameter parameter : parameters) {
            TypeCandidate[] parameterTypeCandidates = parameter.getType().getTypeCandidates();
            for (TypeCandidate parameterTypeCandidate : parameterTypeCandidates) {
                allTypeParametersNotInScope.addAll(Arrays.asList(parameterTypeCandidate.getTypeParameters()));
            }
        }

        if (!isUnit) {
            TypeCandidate[] returnTypeCandidates = returnType.getTypeCandidates();
            for (TypeCandidate returnTypeCandidate : returnTypeCandidates) {
                allTypeParametersNotInScope.addAll(Arrays.asList(returnTypeCandidate.getTypeParameters()));
            }
        }

        List<TypeParameterDescriptor> typeParameters = new ArrayList<TypeParameterDescriptor>(allTypeParametersNotInScope);
        List<String> typeParameterNames = new ArrayList<String>();
        for (TypeParameterDescriptor typeParameter : typeParameters) {
            typeParameterNames.add(getNextAvailableName(typeParameter.getName().getName(), typeParameterNames, scope));
        }
        assert typeParameters.size() == typeParameterNames.size();
        Map<TypeParameterDescriptor, String> typeParameterNameMap = new HashMap<TypeParameterDescriptor, String>();
        for (int i = 0; i < typeParameters.size(); i++) {
            typeParameterNameMap.put(typeParameters.get(i), typeParameterNames.get(i));
        }

        return typeParameterNameMap;
    }

    private void setupTypeReferencesForShortening(
            @NotNull Project project,
            @NotNull JetNamedFunction func,
            @NotNull List<JetTypeReference> typeRefsToShorten,
            @NotNull TypeExpression[] parameterTypeExpressions,
            @Nullable TypeExpression returnTypeExpression
    ) {
        if (isExtension) {
            JetTypeReference receiverTypeRef =
                    JetPsiFactory.createType(project, renderTypeLong(selectedReceiverType.getType(), typeParameterNameMap));
            replaceWithLongerName(project, receiverTypeRef, selectedReceiverType.getType());

            receiverTypeRef = func.getReceiverTypeRef();
            if (receiverTypeRef != null) {
                typeRefsToShorten.add(receiverTypeRef);
            }
        }

        if (!isUnit) {
            assert returnTypeExpression != null;
            JetTypeReference returnTypeRef = func.getReturnTypeRef();
            if (returnTypeRef != null) {
                JetType returnType = returnTypeExpression.getTypeFromSelection(returnTypeRef.getText());
                if (returnType != null) { // user selected a given type
                    replaceWithLongerName(project, returnTypeRef, returnType);
                    returnTypeRef = func.getReturnTypeRef();
                    assert returnTypeRef != null;
                    typeRefsToShorten.add(returnTypeRef);
                }
            }
        }

        List<JetParameter> valueParameters = func.getValueParameters();
        List<Integer> parameterIndicesToShorten = new ArrayList<Integer>();
        assert valueParameters.size() == parameterTypeExpressions.length;
        for (int i = 0; i < valueParameters.size(); i++) {
            JetParameter parameter = valueParameters.get(i);
            JetTypeReference parameterTypeRef = parameter.getTypeReference();
            if (parameterTypeRef != null) {
                JetType parameterType = parameterTypeExpressions[i].getTypeFromSelection(parameterTypeRef.getText());
                if (parameterType != null) {
                    replaceWithLongerName(project, parameterTypeRef, parameterType);
                    parameterIndicesToShorten.add(i);
                }
            }
        }
        valueParameters = func.getValueParameters();
        for (int i : parameterIndicesToShorten) {
            JetTypeReference parameterTypeRef = valueParameters.get(i).getTypeReference();
            if (parameterTypeRef != null) {
                typeRefsToShorten.add(parameterTypeRef);
            }
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
        TypeExpression returnTypeExpression = new TypeExpression(returnType);
        builder.replaceElement(returnTypeRef, returnTypeExpression);
        return returnTypeExpression;
    }

    @NotNull
    private TypeParameterListExpression setupTypeParameterListTemplate(@NotNull TemplateBuilderImpl builder, @NotNull JetNamedFunction func) {
        Map<String, String[]> typeParameterMap = new HashMap<String, String[]>();
        String[] receiverTypeParameterNames = selectedReceiverType.getTypeParameterNames();

        for (Parameter parameter : parameters) {
            TypeCandidate[] parameterTypeCandidates = parameter.getType().getTypeCandidates();
            for (TypeCandidate parameterTypeCandidate : parameterTypeCandidates) {
                typeParameterMap.put(parameterTypeCandidate.getRenderedType(), parameterTypeCandidate.getTypeParameterNames());
            }
        }

        JetTypeReference returnTypeRef = func.getReturnTypeRef();
        if (returnTypeRef != null) {
            TypeCandidate[] returnTypeCandidates = returnType.getTypeCandidates();
            for (TypeCandidate returnTypeCandidate : returnTypeCandidates) {
                typeParameterMap.put(returnTypeCandidate.getRenderedType(), returnTypeCandidate.getTypeParameterNames());
            }
        }

        builder.replaceElement(func, TextRange.create(3, 3), TYPE_PARAMETER_LIST_VARIABLE_NAME, null, false); // ((3, 3) is after "fun")
        return new TypeParameterListExpression(receiverTypeParameterNames, typeParameterMap);
    }

    private TypeExpression[] setupParameterTypeTemplates(@NotNull Project project, @NotNull TemplateBuilder builder,
            @NotNull JetParameterList parameterList) {
        List<JetParameter> jetParameters = parameterList.getParameters();
        assert jetParameters.size() == parameters.size();
        TypeExpression[] parameterTypeExpressions = new TypeExpression[parameters.size()];
        JetNameValidator dummyValidator = JetNameValidator.getEmptyValidator(project);
        for (int i = 0; i < parameters.size(); i++) {
            Parameter parameter = parameters.get(i);
            JetParameter jetParameter = jetParameters.get(i);

            // add parameter type to the template
            parameterTypeExpressions[i] = new TypeExpression(parameter.getType());
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
            for (TypeCandidate typeCandidate : parameter.getType().getTypeCandidates()) {
                String[] suggestedNames = JetNameSuggester.suggestNamesForType(typeCandidate.getType(), dummyValidator);
                parameterTypeToNamesMap.put(typeCandidate.getRenderedType(), suggestedNames);
            }

            // add expression to builder
            Expression parameterNameExpression = new ParameterNameExpression(possibleNames, parameterTypeToNamesMap);
            PsiElement parameterNameIdentifier = jetParameter.getNameIdentifier();
            assert parameterNameIdentifier != null;
            builder.replaceElement(parameterNameIdentifier, parameterNameExpression);
        }
        return parameterTypeExpressions;
    }

    private void replaceWithLongerName(@NotNull Project project, @NotNull JetTypeReference typeRef, @NotNull JetType type) {
        JetTypeReference fullyQualifiedReceiverTypeRef = JetPsiFactory.createType(project, renderTypeLong(type, typeParameterNameMap));
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
    private static String renderDescriptor(
            @NotNull DeclarationDescriptor declarationDescriptor,
            @NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap,
            boolean fq
    ) {
        if (declarationDescriptor instanceof TypeParameterDescriptor) {
            String replacement = typeParameterNameMap.get(declarationDescriptor);
            return replacement == null ? declarationDescriptor.getName().getName() : replacement;
        } else {
            return fq ? DescriptorUtils.getFQName(declarationDescriptor).getFqName() : declarationDescriptor.getName().getName();
        }
    }

    @NotNull
    private static String renderDescriptor(@NotNull DeclarationDescriptor declarationDescriptor, boolean fq) {
        return renderDescriptor(declarationDescriptor, Collections.<TypeParameterDescriptor, String>emptyMap(), fq);
    }

    private static String renderType(@NotNull JetType type, @NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap, boolean fq) {
        List<TypeProjection> projections = type.getArguments();
        DeclarationDescriptor declarationDescriptor = type.getConstructor().getDeclarationDescriptor();
        assert declarationDescriptor != null;
        if (projections.isEmpty()) {
            return renderDescriptor(declarationDescriptor, typeParameterNameMap, fq);
        }

        List<String> arguments = new ArrayList<String>();
        for (TypeProjection projection : projections) {
            arguments.add(renderTypeLong(projection.getType(), typeParameterNameMap));
        }
        return renderDescriptor(declarationDescriptor, typeParameterNameMap, fq) + "<" + StringUtil.join(arguments, ", ") + ">";
    }

    @NotNull
    private static String renderTypeShort(@NotNull JetType type, @NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap) {
        return renderType(type, typeParameterNameMap, false);
    }

    @NotNull
    private static String renderTypeLong(@NotNull JetType type, @NotNull Map<TypeParameterDescriptor, String> typeParameterNameMap) {
        return renderType(type, typeParameterNameMap, true);
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
    private static String getNextAvailableName(@NotNull String name, @NotNull Collection<String> existingNames, @NotNull JetScope scope) {
        if (existingNames.contains(name) || scope.getClassifier(Name.identifier(name)) != null) {
            int j = 1;
            while (existingNames.contains(name + j) || scope.getClassifier(Name.identifier(name + j)) != null) j++;
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
