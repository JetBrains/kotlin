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
import com.intellij.openapi.components.ServiceManager;
import com.intellij.openapi.fileTypes.FileType;
import com.intellij.psi.*;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.FileContentUtilCore;
import com.intellij.util.IncorrectOperationException;
import kotlin.collections.ArraysKt;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtNodeTypes;
import org.jetbrains.kotlin.idea.KotlinFileType;
import org.jetbrains.kotlin.idea.KotlinLanguage;
import org.jetbrains.kotlin.name.FqName;
import org.jetbrains.kotlin.parsing.KotlinParserDefinition;
import org.jetbrains.kotlin.psi.stubs.KotlinFileStub;
import org.jetbrains.kotlin.psi.stubs.elements.KtPlaceHolderStubElementType;
import org.jetbrains.kotlin.psi.stubs.elements.KtStubElementTypes;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class KtFile extends PsiFileBase implements KtDeclarationContainer, KtAnnotated, KtElement, PsiClassOwner, PsiNamedElement {

    private final boolean isCompiled;
    private Boolean isScript = null;

    public KtFile(FileViewProvider viewProvider, boolean compiled) {
        super(viewProvider, KotlinLanguage.INSTANCE);
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
        return KotlinFileType.INSTANCE;
    }

    @Override
    public String toString() {
        return "KtFile: " + getName();
    }

    @NotNull
    @Override
    public List<KtDeclaration> getDeclarations() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            return Arrays.asList(stub.getChildrenByType(KtStubElementTypes.DECLARATION_TYPES, KtDeclaration.ARRAY_FACTORY));
        }
        return PsiTreeUtil.getChildrenOfTypeAsList(this, KtDeclaration.class);
    }

    @Nullable
    public KtImportList getImportList() {
        return findChildByTypeOrClass(KtStubElementTypes.IMPORT_LIST, KtImportList.class);
    }

    @Nullable
    public KtFileAnnotationList getFileAnnotationList() {
        return findChildByTypeOrClass(KtStubElementTypes.FILE_ANNOTATION_LIST, KtFileAnnotationList.class);
    }

    @Nullable
    public <T extends KtElementImplStub<? extends StubElement<?>>> T findChildByTypeOrClass(
            @NotNull KtPlaceHolderStubElementType<T> elementType,
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
    public List<KtImportDirective> getImportDirectives() {
        KtImportList importList = getImportList();
        return importList != null ? importList.getImports() : Collections.emptyList();
    }

    @Nullable
    public KtImportDirective findImportByAlias(@NotNull String name) {
        for (KtImportDirective directive : getImportDirectives()) {
            if (name.equals(directive.getAliasName())) {
                return directive;
            }
        }
        return null;
    }

    // scripts have no package directive, all other files must have package directives
    @Nullable
    public KtPackageDirective getPackageDirective() {
        KotlinFileStub stub = getStub();
        if (stub != null) {
            StubElement<KtPackageDirective> packageDirectiveStub = stub.findChildStubByType(KtStubElementTypes.PACKAGE_DIRECTIVE);
            return packageDirectiveStub != null ? packageDirectiveStub.getPsi() : null;
        }
        return getPackageDirectiveByTree();
    }

    @Nullable
    private KtPackageDirective getPackageDirectiveByTree() {
        ASTNode ast = getNode().findChildByType(KtNodeTypes.PACKAGE_DIRECTIVE);
        return ast != null ? (KtPackageDirective) ast.getPsi() : null;
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
        KtPackageDirective packageDirective = getPackageDirectiveByTree();
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
        KtFileClassProvider fileClassProvider = ServiceManager.getService(getProject(), KtFileClassProvider.class);
        // TODO We don't currently support finding light classes for scripts
        if (fileClassProvider != null && !isScript()) {
            return fileClassProvider.getFileClasses(this);
        }
        return PsiClass.EMPTY_ARRAY;
    }

    @Override
    public void setPackageName(String packageName) { }

    @Override
    public void clearCaches() {
        super.clearCaches();
        isScript = null;
    }

    @Nullable
    public KtScript getScript() {
        if (isScript != null && !isScript) return null;

        KtScript result = PsiTreeUtil.getChildOfType(this, KtScript.class);
        if (isScript == null) {
            isScript = result != null;
        }

        return result;
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
        if (visitor instanceof KtVisitor) {
            accept((KtVisitor) visitor, null);
        }
        else {
            visitor.visitFile(this);
        }
    }

    @NotNull
    @Override
    public KtFile getContainingKtFile() {
        return this;
    }

    @Override
    public <D> void acceptChildren(@NotNull KtVisitor<Void, D> visitor, D data) {
        KtPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitKtFile(this, data);
    }

    @NotNull
    @Override
    public List<KtAnnotation> getAnnotations() {
        KtFileAnnotationList fileAnnotationList = getFileAnnotationList();
        if (fileAnnotationList == null) return Collections.emptyList();

        return fileAnnotationList.getAnnotations();
    }

    @NotNull
    @Override
    public List<KtAnnotationEntry> getAnnotationEntries() {
        KtFileAnnotationList fileAnnotationList = getFileAnnotationList();
        if (fileAnnotationList == null) return Collections.emptyList();

        return fileAnnotationList.getAnnotationEntries();
    }

    /**
     * @return annotations that do not belong to any declaration due to incomplete code or syntax errors
     */
    @NotNull
    public List<KtAnnotationEntry> getDanglingAnnotations() {
        KotlinFileStub stub = getStub();
        KtModifierList[] danglingModifierLists = stub == null
                                                 ? findChildrenByClass(KtModifierList.class)
                                                 : stub.getChildrenByType(
                                                         KtStubElementTypes.MODIFIER_LIST,
                                                         KtStubElementTypes.MODIFIER_LIST.getArrayFactory()
                                                 );
        return ArraysKt.flatMap(danglingModifierLists, KtModifierList::getAnnotationEntries);
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {
        PsiElement result = super.setName(name);
        boolean willBeScript = name.endsWith(KotlinParserDefinition.STD_SCRIPT_EXT);
        if (isScript() != willBeScript) {
            FileContentUtilCore.reparseFiles(CollectionsKt.listOfNotNull(getVirtualFile()));
        }
        return result;
    }

    @NotNull
    @Override
    public KtElement getPsiOrParent() {
        return this;
    }
}
