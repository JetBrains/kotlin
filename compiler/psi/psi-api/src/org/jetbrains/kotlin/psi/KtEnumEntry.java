/*
 * Copyright 2010-2026 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.kotlin.KtStubBasedElementTypes;
import org.jetbrains.kotlin.lexer.KtTokens;
import org.jetbrains.kotlin.name.ClassId;
import org.jetbrains.kotlin.psi.psiUtil.KtPsiUtilKt;
import org.jetbrains.kotlin.psi.stubs.KotlinClassStub;

import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents an {@code enum} entry within an {@code enum} class.
 *
 * <h3>Example:</h3>
 * <pre>{@code
 * enum class Color {
 *     RED,
 * //  ^_^
 *     GREEN,
 *     BLUE
 * }
 * }</pre>
 */
public class KtEnumEntry extends KtClass implements KtDeclarationWithReturnType {
    public KtEnumEntry(@NotNull ASTNode node) {
        super(node);
    }

    public KtEnumEntry(@NotNull KotlinClassStub stub) {
        super(stub, KtStubBasedElementTypes.ENUM_ENTRY);
    }

    @NotNull
    @Override
    public List<KtSuperTypeListEntry> getSuperTypeListEntries() {
        KtInitializerList initializerList = getInitializerList();
        if (initializerList == null) {
            return Collections.emptyList();
        }
        return initializerList.getInitializers();
    }

    public boolean hasInitializer() {
        return !getSuperTypeListEntries().isEmpty();
    }

    @Nullable
    @Override
    public ClassId getClassId() {
        return null;
    }

    @Nullable
    @SuppressWarnings("deprecation") // KT-78356
    public KtInitializerList getInitializerList() {
        return getStubOrPsiChild(KtStubBasedElementTypes.INITIALIZER_LIST);
    }

    @Override
    public boolean isEquivalentTo(@Nullable PsiElement another) {
        if (this == another) return true;
        if (!(another instanceof KtEnumEntry)) return false;
        KtEnumEntry anotherEnumEntry = (KtEnumEntry) another;

        if (!Objects.equals(getName(), anotherEnumEntry.getName())) return false;

        KtClassOrObject thisContainingClass = KtPsiUtilKt.getContainingClassOrObject(this);
        if (thisContainingClass == null) return false;

        KtClassOrObject anotherContainingClass = KtPsiUtilKt.getContainingClassOrObject(anotherEnumEntry);
        return thisContainingClass.isEquivalentTo(anotherContainingClass);
    }

    /**
     * Get the comma token if present.
     *
     * @return Comma token or null.
     */
    @Nullable
    public PsiElement getComma() {
        return this.findLastChildByType(KtTokens.COMMA);
    }

    /**
     * Get the semicolon token if present.
     *
     * @return Semicolon token or null.
     */
    @Nullable
    public PsiElement getSemicolon() {
        return this.findLastChildByType(KtTokens.SEMICOLON);
    }

    /**
     * Adds a semicolon to the end of the enum entry if not already present.
     * <p>
     * This method might move the semicolon from its next sibling if the semicolon was
     * previously a child of the parent {@link KtClassBody}.
     * <p>
     * This can happen if we called {@link KtClassOrObject#addDeclaration(KtDeclaration)}
     * for a {@link KtFunction} before inserting the {@link KtEnumEntry} into the {@link KtClassBody},
     * and the semicolon was previously a child of the parent {@link KtClassBody} rather than the last
     * {@link KtEnumEntry}. Or we deleted the last {@link KtEnumEntry} (reparenting the
     * semicolon onto the parent {@link KtClassBody}) and inserted a new {@link KtEnumEntry}.
     *
     * @return The added (or existing) semicolon element.
     * @deprecated Use {@code org.jetbrains.kotlin.idea.base.psi.KotlinPsiModificationUtils.addEnumEntrySemicolon(this)}
     * instead.
     */
    @KtExperimentalApi
    @NotNull
    @Deprecated
    public PsiElement addSemicolon() {
        return KtPsiMutationService.getInstance().addEnumEntrySemicolon(this);
    }

    @Override
    public void delete() {
        KtPsiMutationService.getInstance().deleteEnumEntry(this);
    }

    @Override
    public <R, D> R accept(@NotNull KtVisitor<R, D> visitor, D data) {
        return visitor.visitEnumEntry(this, data);
    }

    @Nullable
    @Override
    public KtTypeReference getTypeReference() {
        return null;
    }
}
