/*
 * Copyright 2010-2017 JetBrains s.r.o.
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

package org.jetbrains.kotlin.psi.pattern

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.search.LocalSearchScope
import com.intellij.psi.search.SearchScope
import com.intellij.psi.tree.TokenSet
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.kdoc.psi.api.KDoc
import org.jetbrains.kotlin.lexer.KtModifierKeywordToken
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.VAL_KEYWORD
import org.jetbrains.kotlin.lexer.KtTokens.VAR_KEYWORD
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.addRemoveModifier.addAnnotationEntry
import org.jetbrains.kotlin.psi.addRemoveModifier.addModifier
import org.jetbrains.kotlin.psi.addRemoveModifier.removeModifier
import org.jetbrains.kotlin.psi.findDocComment.findDocComment
import org.jetbrains.kotlin.psi.typeRefHelpers.setTypeReference
import org.jetbrains.kotlin.resolve.calls.smartcasts.ConditionalDataFlowInfo
import org.jetbrains.kotlin.resolve.calls.util.isSingleUnderscore
import org.jetbrains.kotlin.types.expressions.ConditionalTypeInfo
import org.jetbrains.kotlin.types.expressions.PatternResolveState
import org.jetbrains.kotlin.types.expressions.PatternResolver

class KtPatternVariableDeclaration(node: ASTNode) : KtPatternElement(node), KtVariableDeclaration {
    override fun getName(): String? {
        val identifier = nameIdentifier
        if (identifier != null) {
            val text = identifier.text
            return if (text != null) KtPsiUtil.unquoteIdentifier(text) else null
        }
        else {
            return null
        }
    }

    override fun getNameAsName(): Name? {
        val name = name
        return if (name != null) Name.identifier(name) else null
    }

    override fun getNameAsSafeName(): Name {
        return KtPsiUtil.safeName(name)
    }

    override fun getNameIdentifier(): PsiElement? {
        return findChildByType(KtTokens.IDENTIFIER)
    }

    override fun setName(name: String): PsiElement {
        return nameIdentifier!!.replace(KtPsiFactory(this).createNameIdentifier(name))
    }

    override fun getTextOffset(): Int {
        return nameIdentifier?.textRange?.startOffset ?: textRange.startOffset
    }

    override fun getModifierList(): KtModifierList? {
        return findChildByType<PsiElement>(KtNodeTypes.MODIFIER_LIST) as KtModifierList?
    }

    override fun hasModifier(modifier: KtModifierKeywordToken): Boolean {
        return modifierList?.hasModifier(modifier) != null
    }

    override fun addModifier(modifier: KtModifierKeywordToken) {
        addModifier(this, modifier)
    }

    override fun removeModifier(modifier: KtModifierKeywordToken) {
        removeModifier(this, modifier)
    }

    override fun addAnnotationEntry(annotationEntry: KtAnnotationEntry): KtAnnotationEntry {
        return addAnnotationEntry(this, annotationEntry)
    }

    override fun getAnnotationEntries(): List<KtAnnotationEntry> {
        val modifierList = modifierList ?: return emptyList()
        return modifierList.annotationEntries
    }

    override fun getAnnotations(): List<KtAnnotation> {
        val modifierList = modifierList ?: return emptyList()
        return modifierList.annotations
    }

    override fun getDocComment(): KDoc? {
        return findDocComment(this)
    }

    override fun getTypeReference(): KtTypeReference? {
        return patternTypeReference?.typeReference
    }

    override fun setTypeReference(typeRef: KtTypeReference?): KtTypeReference? {
        return setTypeReference(this, nameIdentifier, typeRef)
    }

    override fun getColon(): PsiElement? {
        return findChildByType(KtTokens.COLON)
    }

    override fun getValueParameterList(): KtParameterList? {
        return null
    }

    override fun getValueParameters(): List<KtParameter> {
        return emptyList()
    }

    override fun getReceiverTypeReference(): KtTypeReference? {
        return null
    }

    override fun getTypeParameterList(): KtTypeParameterList? {
        return null
    }

    override fun getTypeConstraintList(): KtTypeConstraintList? {
        return null
    }

    override fun getTypeConstraints(): List<KtTypeConstraint> {
        return emptyList()
    }

    override fun getTypeParameters(): List<KtTypeParameter> {
        return emptyList()
    }

    override fun isVar(): Boolean {
        return findChildByType<PsiElement?>(KtTokens.VAR_KEYWORD) != null
    }

    override fun getInitializer(): KtExpression? {
        return null
    }

    override fun hasInitializer(): Boolean {
        return false
    }

    override fun getValOrVarKeyword(): PsiElement? {
        return findChildByType<PsiElement?>(TokenSet.create(VAL_KEYWORD, VAR_KEYWORD))
    }

    override fun getFqName(): FqName? {
        return null
    }

    override fun getUseScope(): SearchScope {
        var enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration(this, false)
        if (enclosingBlock is KtParameter) {
            enclosingBlock = KtPsiUtil.getEnclosingElementForLocalDeclaration((enclosingBlock as KtParameter?)!!, false)
        }
        return if (enclosingBlock != null) LocalSearchScope(enclosingBlock) else super.getUseScope()
    }

    override fun <R, D> accept(visitor: KtVisitor<R, D>, data: D): R {
        return visitor.visitPatternVariableDeclaration(this, data)
    }

    val isEmpty: Boolean
        get() = isSingleUnderscore && patternTypeReference == null && constraint == null

    val patternTypeReference: KtPatternTypeReference?
        get() = findChildByType(KtNodeTypes.PATTERN_TYPE_REFERENCE)

    val constraint: KtPatternConstraint?
        get() = findChildByType(KtNodeTypes.PATTERN_CONSTRAINT)

    val parentEntry: KtPatternEntry?
        get() = (parent as? KtPatternEntry)

    override fun getTypeInfo(resolver: PatternResolver, state: PatternResolveState) = resolver.restoreOrCreate(this, state) {
        val typeReferenceInfo = patternTypeReference?.getTypeInfo(resolver, state)
        val constraintInfo = constraint?.getTypeInfo(resolver, state)
        val constraintType = constraintInfo?.type
        val typeReferenceType = typeReferenceInfo?.type
        (typeReferenceType ?: constraintType)?.let {
            val info = ConditionalTypeInfo(it, ConditionalDataFlowInfo.EMPTY)
            info.and(typeReferenceInfo, constraintInfo)
        }
    }

    override fun resolve(resolver: PatternResolver, state: PatternResolveState): ConditionalTypeInfo {
        val typeInfo = patternTypeReference?.resolve(resolver, state)
        val constraintInfo = constraint?.resolve(resolver, state)
        val info = resolver.resolveType(this, state)
        val defineInfo = resolver.defineVariable(this, state)
        return info.and(typeInfo, constraintInfo, defineInfo)
    }
}
