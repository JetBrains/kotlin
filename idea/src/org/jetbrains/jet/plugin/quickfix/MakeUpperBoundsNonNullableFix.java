package org.jetbrains.jet.plugin.quickfix;

import com.intellij.codeInsight.intention.IntentionAction;
import com.intellij.openapi.editor.Editor;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.util.Pair;
import com.intellij.psi.PsiElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.lang.descriptors.TypeParameterDescriptor;
import org.jetbrains.jet.lang.diagnostics.Diagnostic;
import org.jetbrains.jet.lang.psi.*;
import org.jetbrains.jet.lang.resolve.BindingContext;
import org.jetbrains.jet.lang.types.JetType;
import org.jetbrains.jet.lang.types.lang.KotlinBuiltIns;
import org.jetbrains.jet.plugin.JetBundle;
import org.jetbrains.jet.plugin.project.AnalyzerFacadeWithCache;

import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;

public class MakeUpperBoundsNonNullableFix extends JetIntentionAction<JetTypeReference> {

    public MakeUpperBoundsNonNullableFix(@NotNull JetTypeReference element) {
        super(element);
    }

    @NotNull
    @Override
    public String getText() {
        return JetBundle.message("make.all.upper.bounds.non.nullable");
    }

    @NotNull
    @Override
    public String getFamilyName() {
        return JetBundle.message("make.all.upper.bounds.non.nullable.family");
    }

    @Override
    protected void invoke(@NotNull Project project, Editor editor, JetFile file) throws IncorrectOperationException {
        Queue<JetTypeParameter> typeParameterQueue = new LinkedList<JetTypeParameter>();
        Collection<JetTypeParameter> queuedTypeParameters = new HashSet<JetTypeParameter>();

        JetTypeParameter typeParameterForElement = getCorrespondingTypeParameter(element);
        typeParameterQueue.add(typeParameterForElement);
        queuedTypeParameters.add(typeParameterForElement);

        while (!typeParameterQueue.isEmpty()) {
            JetTypeParameter typeParameter = typeParameterQueue.poll();
            JetTypeReference extendsBoundTypeReference = typeParameter.getExtendsBound();
            String typeParameterName = typeParameter.getName();
            JetTypeParameterListOwner typeParameterListOwner = PsiTreeUtil.getParentOfType(typeParameter, JetTypeParameterListOwner.class);
            assert typeParameterName != null : "Type parameter without name";
            assert typeParameterListOwner != null : "Type parameter outside any type parameter list owner";

            if (extendsBoundTypeReference == null) {
                Pair<PsiElement, PsiElement> typeWithColon =
                        JetPsiFactory.createTypeWhiteSpaceAndColon(project, KotlinBuiltIns.getInstance().getAnyType().toString());
                typeParameter.addRangeAfter(typeWithColon.first, typeWithColon.second, typeParameter.getLastChild());
            }
            else {
                makeNotNullable(extendsBoundTypeReference);
                JetTypeParameter typeParameterForUpperBound = getCorrespondingTypeParameter(extendsBoundTypeReference);
                if (typeParameterForUpperBound != null && !queuedTypeParameters.contains(typeParameterForUpperBound)) {
                    typeParameterQueue.add(typeParameterForUpperBound);
                    queuedTypeParameters.add(typeParameterForUpperBound);
                }
            }

            for (JetTypeConstraint typeConstraint: typeParameterListOwner.getTypeConstraints()) {
                JetSimpleNameExpression subjectTypeParameterName = typeConstraint.getSubjectTypeParameterName();
                if (subjectTypeParameterName != null && typeParameterName.equals(subjectTypeParameterName.getReferencedName())) {
                    JetTypeReference boundTypeReferenceFromConstraint = typeConstraint.getBoundTypeReference();
                    assert boundTypeReferenceFromConstraint != null : "Type constraint without bound type reference after parsing";
                    makeNotNullable(boundTypeReferenceFromConstraint);
                    JetTypeParameter typeParameterForBoundFromConstraint = getCorrespondingTypeParameter(boundTypeReferenceFromConstraint);
                    if (typeParameterForBoundFromConstraint != null && !queuedTypeParameters.contains(typeParameterForBoundFromConstraint)) {
                        queuedTypeParameters.add(typeParameterForBoundFromConstraint);
                        typeParameterQueue.add(typeParameterForBoundFromConstraint);
                    }
                }
            }
        }
    }

    public static void makeNotNullable(@NotNull JetTypeReference typeReference) {
        JetTypeElement typeElement = typeReference.getTypeElement();
        while (typeElement instanceof JetNullableType) {
            typeElement.replace(((JetNullableType) typeElement).getInnerType());
            typeElement = typeReference.getTypeElement();
        }
    }

    @Nullable
    public static JetTypeParameter getCorrespondingTypeParameter(@NotNull JetTypeReference genericTypeRef) {
        BindingContext context = AnalyzerFacadeWithCache.analyzeFileWithCache((JetFile) genericTypeRef.getContainingFile()).getBindingContext();
        JetType type = context.get(BindingContext.TYPE, genericTypeRef);
        if (type == null) return null;

        JetTypeParameterListOwner typeParameterListOwner = PsiTreeUtil.getParentOfType(genericTypeRef, JetTypeParameterListOwner.class);
        while (typeParameterListOwner != null) {
            for (JetTypeParameter typeParameter: typeParameterListOwner.getTypeParameters()) {
                TypeParameterDescriptor typeParameterDescriptor = context.get(BindingContext.TYPE_PARAMETER, typeParameter);
                if (typeParameterDescriptor != null && type.getConstructor() == typeParameterDescriptor.getTypeConstructor()) {
                    return typeParameter;
                }
            }
            typeParameterListOwner = PsiTreeUtil.getParentOfType(typeParameterListOwner, JetTypeParameterListOwner.class, true);
        }
        return null;
    }

    public static JetIntentionActionFactory createFactory() {
        return new JetIntentionActionFactory() {
            @Nullable
            @Override
            public IntentionAction createAction(Diagnostic diagnostic) {
                JetTypeReference typeReference = QuickFixUtil.getParentElementOfType(diagnostic, JetTypeReference.class);
                assert typeReference != null;
                return new MakeUpperBoundsNonNullableFix(typeReference);
            }
        };
    }
}
