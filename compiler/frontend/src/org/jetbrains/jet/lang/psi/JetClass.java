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

package org.jetbrains.jet.lang.psi;

import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.openapi.util.text.StringUtil;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.jet.JetNodeTypes;
import org.jetbrains.jet.lang.psi.stubs.PsiJetClassStub;
import org.jetbrains.jet.lang.psi.stubs.elements.JetStubElementTypes;
import org.jetbrains.jet.lang.resolve.name.FqName;
import org.jetbrains.jet.lexer.JetTokens;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class JetClass extends JetTypeParameterListOwnerStub<PsiJetClassStub> implements JetClassOrObject {

    public JetClass(@NotNull ASTNode node) {
        super(node);
    }

    public JetClass(@NotNull final PsiJetClassStub stub) {
        super(stub, JetStubElementTypes.CLASS);
    }

    @NotNull
    @Override
    public List<JetDeclaration> getDeclarations() {
        JetClassBody body = (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
        if (body == null) return Collections.emptyList();

        return body.getDeclarations();
    }

    @Override
    public void accept(@NotNull JetVisitorVoid visitor) {
        visitor.visitClass(this);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitClass(this, data);
    }

    @Nullable
    public JetParameterList getPrimaryConstructorParameterList() {
        return (JetParameterList) findChildByType(JetNodeTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<JetParameter> getPrimaryConstructorParameters() {
        JetParameterList list = getPrimaryConstructorParameterList();
        if (list == null) return Collections.emptyList();
        return list.getParameters();
    }

    @Override
    @Nullable
    public JetDelegationSpecifierList getDelegationSpecifierList() {
        return (JetDelegationSpecifierList) findChildByType(JetNodeTypes.DELEGATION_SPECIFIER_LIST);
    }

    @Override
    @NotNull
    public List<JetDelegationSpecifier> getDelegationSpecifiers() {
        JetDelegationSpecifierList list = getDelegationSpecifierList();
        return list != null ? list.getDelegationSpecifiers() : Collections.<JetDelegationSpecifier>emptyList();
    }

    @Nullable
    public JetModifierList getPrimaryConstructorModifierList() {
        return (JetModifierList) findChildByType(JetNodeTypes.PRIMARY_CONSTRUCTOR_MODIFIER_LIST);
    }

    @Override
    @NotNull
    public List<JetClassInitializer> getAnonymousInitializers() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getAnonymousInitializers();
    }

    @Override
    public boolean hasPrimaryConstructor() {
        return getPrimaryConstructorParameterList() != null;
    }

    @Override
    public JetObjectDeclarationName getNameAsDeclaration() {
        return (JetObjectDeclarationName) findChildByType(JetNodeTypes.OBJECT_DECLARATION_NAME);
    }

    @Override
    public JetClassBody getBody() {
        return (JetClassBody) findChildByType(JetNodeTypes.CLASS_BODY);
    }

    @Nullable
    public JetClassObject getClassObject() {
        JetClassBody body = getBody();
        if (body == null) return null;
        return body.getClassObject();
    }

    public List<JetProperty> getProperties() {
        JetClassBody body = getBody();
        if (body == null) return Collections.emptyList();

        return body.getProperties();
    }

    public boolean isTrait() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            return stub.isTrait();
        }

        return findChildByType(JetTokens.TRAIT_KEYWORD) != null;
    }

    public boolean isEnum() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            return stub.isEnumClass();
        }

        return hasModifier(JetTokens.ENUM_KEYWORD);
    }

    public boolean isAnnotation() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            return stub.isAnnotation();
        }

        return hasModifier(JetTokens.ANNOTATION_KEYWORD);
    }

    public boolean isInner() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            return stub.isInner();
        }

        return hasModifier(JetTokens.INNER_KEYWORD);
    }

    @NotNull
    @Override
    public IStubElementType getElementType() {
        return JetStubElementTypes.CLASS;
    }

    @Override
    public void delete() throws IncorrectOperationException {
        JetPsiUtil.deleteClass(this);
    }

    @Override
    public boolean isEquivalentTo(PsiElement another) {
        if (super.isEquivalentTo(another)) {
            return true;
        }
        if (another instanceof JetClass) {
            String fq1 = getQualifiedName();
            String fq2 = ((JetClass) another).getQualifiedName();
            return fq1 != null && fq2 != null && fq1.equals(fq2);
        }
        return false;
    }

    @Nullable
    private String getQualifiedName() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            FqName fqName = stub.getFqName();
            return fqName == null ? null : fqName.getFqName();
        }

        List<String> parts = new ArrayList<String>();
        JetClassOrObject current = this;
        while (current != null) {
            parts.add(current.getName());
            current = PsiTreeUtil.getParentOfType(current, JetClassOrObject.class);
        }
        PsiFile file = getContainingFile();
        if (!(file instanceof JetFile)) return null;
        String fileQualifiedName = ((JetFile) file).getNamespaceHeader().getQualifiedName();
        if (!fileQualifiedName.isEmpty()) {
            parts.add(fileQualifiedName);
        }
        Collections.reverse(parts);
        return StringUtil.join(parts, ".");
    }

    /**
     * Returns the list of unqualified names that are indexed as the superclass names of this class. For the names that might be imported
     * via an aliased import, includes both the original and the aliased name (reference resolution during inheritor search will sort this out).
     *
     * @return the list of possible superclass names
     */
    @NotNull
    public List<String> getSuperNames() {
        PsiJetClassStub stub = getStub();
        if (stub != null) {
            return stub.getSuperNames();
        }

        final List<JetDelegationSpecifier> specifiers = getDelegationSpecifiers();
        if (specifiers.size() == 0) return Collections.emptyList();
        List<String> result = new ArrayList<String>();
        for (JetDelegationSpecifier specifier : specifiers) {
            final JetUserType superType = specifier.getTypeAsUserType();
            if (superType != null) {
                final String referencedName = superType.getReferencedName();
                if (referencedName != null) {
                    addSuperName(result, referencedName);
                }
            }
        }
        return result;
    }

    private void addSuperName(List<String> result, String referencedName) {
        result.add(referencedName);
        if (getContainingFile() instanceof JetFile) {
            final JetImportDirective directive = ((JetFile) getContainingFile()).findImportByAlias(referencedName);
            if (directive != null) {
                JetExpression reference = directive.getImportedReference();
                while (reference instanceof JetDotQualifiedExpression) {
                    reference = ((JetDotQualifiedExpression) reference).getSelectorExpression();
                }
                if (reference instanceof JetSimpleNameExpression) {
                    result.add(((JetSimpleNameExpression) reference).getReferencedName());
                }
            }
        }
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }
}
