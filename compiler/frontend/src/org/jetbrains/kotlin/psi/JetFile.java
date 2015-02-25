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

package org.jetbrains.kotlin.psi;

import com.intellij.extapi.psi.PsiFileBase;
import com.intellij.lang.ASTNode;
import com.intellij.lang.FileASTNode;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.FileViewProvider;
import com.intellij.psi.PsiClass;
import com.intellij.psi.PsiClassOwner;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import kotlin.Function1;
import kotlin.KotlinPackage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.JetNodeTypes;
import org.jetbrains.kotlin.idea.JetFileType;
import org.jetbrains.kotlin.idea.JetLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.elements.JetPlaceHolderStubElementType;
import org.jetbrains.kotlin.psi.stubs.elements.JetStubElementTypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class JetFile extends PsiFileBase implements JetDeclarationContainer, JetAnnotated, JetElement, PsiClassOwner {

    private final boolean isCompiled;

    public JetFile(FileViewProvider viewProvider, boolean compiled) {
        super(viewProvider, JetLanguage.INSTANCE);
        this.isCompiled = compiled;
    }

    @Override
    public FileASTNode getNode() {
        return super.getNode();
    }

    public boolean isCompiled() {
        return isCompiled;
    }

    @Override
    @NotNull
    public FileType getFileType() {
        return JetFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "JetFile: " + getName();
    }

    @NotNull
    @Override
    public List<JetDeclaration> getDeclarations() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            return Arrays.asList(stub.getChildrenByType(JetStubElementTypes.DECLARATION_TYPES, JetDeclaration.ARRAY_FACTORY));
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(this, JetDeclaration.class);
    }

    @Nullable
    public JetImportList getImportList() {
        return findChildByTypeOrClass(JetStubElementTypes.IMPORT_LIST, JetImportList.class);
    }

    @Nullable
    public JetFileAnnotationList getFileAnnotationList() {
        return findChildByTypeOrClass(JetStubElementTypes.FILE_ANNOTATION_LIST, JetFileAnnotationList.class);
    }

    @Nullable
    public <T extends JetElementImplStub<? extends StubElement<?>>> T findChildByTypeOrClass(
            @NotNull JetPlaceHolderStubElementType<T> elementType,
            @NotNull Class<T> elementClass
    ) {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            StubElement<T> importListStub = stub.findChildStubByType(elementType);
            return importListStub != null ? importListStub.getPsi() : null;
        }
        return findChildByClass(elementClass);
    }

    @NotNull
    public List<JetImportDirective> getImportDirectives() {
        JetImportList importList = getImportList();
        return importList != null ? importList.getImports() : Collections.<JetImportDirective>emptyList();
    }

    @Nullable
    public JetImportDirective findImportByAlias(@NotNull String name) {
        for (JetImportDirective directive : getImportDirectives()) {
            if (name.equals(directive.getAliasName())) {
                return directive;
            }
        }
        return null;
    }

    // scripts have no package directive, all other files must have package directives
    @Nullable
    public JetPackageDirective getPackageDirective() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            StubElement<JetPackageDirective> packageDirectiveStub = stub.findChildStubByType(JetStubElementTypes.PACKAGE_DIRECTIVE);
            return packageDirectiveStub != null ? packageDirectiveStub.getPsi() : null;
        }
        ASTNode ast = getNode().findChildByType(JetNodeTypes.PACKAGE_DIRECTIVE);
        return ast != null ? (JetPackageDirective) ast.getPsi() : null;
    }

    @Deprecated // getPackageFqName should be used instead
    @Override
    @NotNull
    public String getPackageName() {
        return getPackageFqName().asString();
    }

    @NotNull
    public FqName getPackageFqName() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            return stub.getPackageFqName();
        }
        return getPackageFqNameByTree();
    }

    @NotNull
    public FqName getPackageFqNameByTree() {
        JetPackageDirective packageDirective = getPackageDirective();
        if (packageDirective == null) {
            return FqName.ROOT;
        }
        return packageDirective.getFqName();
    }

    @Override
    @Nullable
    public KotlinFileStub getStub() {
        return (KotlinFileStub) super.getStub();
    }

    @NotNull
    @Override
    public PsiClass[] getClasses() {
        return PsiClass.EMPTY_ARRAY;
    }

    @Override
    public void setPackageName(String packageName) { }

    // SCRIPT: find script in file
    @Nullable
    public JetScript getScript() {
        return PsiTreeUtil.getChildOfType(this, JetScript.class);
    }

    public boolean isScript() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            return stub.isScript();
        }
        return isScriptByTree();
    }

    public boolean isScriptByTree() {
        return getScript() != null;
    }

    @NotNull
    @Override
    public String getName() {
        return super.getName(); // TODO
    }

    @Override
    public void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof JetVisitor) {
            accept((JetVisitor) visitor, null);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @NotNull
    @Override
    public JetFile getContainingJetFile() {
        return this;
    }

    @Override
    public <D> void acceptChildren(@NotNull JetVisitor<Void, D> visitor, D data) {
        JetPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public <R, D> R accept(@NotNull JetVisitor<R, D> visitor, D data) {
        return visitor.visitJetFile(this, data);
    }

    @NotNull
    @Override
    public List<JetAnnotation> getAnnotations() {
        JetFileAnnotationList fileAnnotationList = getFileAnnotationList();
        if (fileAnnotationList == null) return Collections.emptyList();

        return fileAnnotationList.getAnnotations();
    }

    @NotNull
    @Override
    public List<JetAnnotationEntry> getAnnotationEntries() {
        JetFileAnnotationList fileAnnotationList = getFileAnnotationList();
        if (fileAnnotationList == null) return Collections.emptyList();

        return fileAnnotationList.getAnnotationEntries();
    }

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    @NotNull
    public List<JetAnnotationEntry> getDanglingAnnotations() {
        KotlinFileStub stub = getStub();
        JetModifierList[] danglingModifierLists = stub == null
                                                  ? findChildrenByClass(JetModifierList.class)
                                                  : stub.getChildrenByType(
                                                          JetStubElementTypes.MODIFIER_LIST,
                                                          JetStubElementTypes.MODIFIER_LIST.getArrayFactory()
                                                  );
        return KotlinPackage.flatMap(
                danglingModifierLists,
                new Function1<JetModifierList, Iterable<JetAnnotationEntry>>() {
                    @Override
                    public Iterable<JetAnnotationEntry> invoke(JetModifierList modifierList) {
                        return modifierList.getAnnotationEntries();
                    }
                });
    }
}
