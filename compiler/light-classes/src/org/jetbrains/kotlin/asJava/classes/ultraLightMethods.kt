/*
 * Copyright 2010-2019 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.psi.*
import com.intellij.psi.impl.PsiImplUtil
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.light.LightMethodBuilder
import com.intellij.psi.impl.light.LightTypeParameterListBuilder
import com.intellij.psi.util.MethodSignatureBackedByPsiMethod
import org.jetbrains.kotlin.asJava.builder.LightMemberOriginForDeclaration
import org.jetbrains.kotlin.asJava.builder.MemberIndex
import org.jetbrains.kotlin.asJava.elements.KtLightAbstractAnnotation
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.codegen.FunctionCodegen
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtTypeParameterListOwner
import org.jetbrains.kotlin.resolve.jvm.diagnostics.JvmDeclarationOriginKind
import org.jetbrains.kotlin.types.KotlinType

internal abstract class KtUltraLightMethod(
    internal val delegate: LightMethodBuilder,
    closestDeclarationForOrigin: KtDeclaration?,
    protected val support: KtUltraLightSupport,
    containingClass: KtUltraLightClass
) : KtLightMethodImpl(
    { delegate },
    closestDeclarationForOrigin?.let {
        LightMemberOriginForDeclaration(it, JvmDeclarationOriginKind.OTHER)
    },
    containingClass
), KtUltraLightElementWithNullabilityAnnotation<KtDeclaration, PsiMethod> {

    override val memberIndex: MemberIndex? = null

    override val psiTypeForNullabilityAnnotation: PsiType?
        get() = returnType

    // These two overrides are necessary because ones from KtLightMethodImpl suppose that clsDelegate.returnTypeElement is valid
    // While here we only set return type for LightMethodBuilder (see org.jetbrains.kotlin.asJava.classes.KtUltraLightClass.asJavaMethod)
    override fun getReturnTypeElement(): PsiTypeElement? = null

    override fun getReturnType(): PsiType? = clsDelegate.returnType

    override fun buildParametersForList(): List<PsiParameter> = clsDelegate.parameterList.parameters.toList()

    // should be in super
    override fun isVarArgs() = PsiImplUtil.isVarArgs(this)

    abstract override fun buildTypeParameterList(): PsiTypeParameterList

    private val _throwsList: PsiReferenceList by lazyPub {
        val list =
            object : KotlinLightReferenceListBuilder(manager, language, PsiReferenceList.Role.THROWS_LIST) {
                override fun getParent() = this@KtUltraLightMethod
                override fun getContainingFile() = this@KtUltraLightMethod.containingFile
            }
        computeDescriptor()?.let {
            for (ex in FunctionCodegen.getThrownExceptions(it)) {
                val psiClassType = ex.defaultType.asPsiType(support, TypeMappingMode.DEFAULT, list) as? PsiClassType ?: continue
                list.addReference(psiClassType)
            }
        }
        list
    }

    override fun getHierarchicalMethodSignature() = PsiSuperMethodImplUtil.getHierarchicalMethodSignature(this)

    override fun findSuperMethodSignaturesIncludingStatic(checkAccess: Boolean): List<MethodSignatureBackedByPsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethodSignaturesIncludingStatic(this, checkAccess)

    override fun findDeepestSuperMethod() = PsiSuperMethodImplUtil.findDeepestSuperMethod(this)

    override fun findDeepestSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findDeepestSuperMethods(this)

    override fun findSuperMethods(): Array<out PsiMethod> = PsiSuperMethodImplUtil.findSuperMethods(this)

    override fun findSuperMethods(checkAccess: Boolean): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, checkAccess)

    override fun findSuperMethods(parentClass: PsiClass?): Array<out PsiMethod> =
        PsiSuperMethodImplUtil.findSuperMethods(this, parentClass)

    override fun getThrowsList(): PsiReferenceList = _throwsList

    abstract fun computeDescriptor(): FunctionDescriptor?

    override fun equals(other: Any?): Boolean = this === other

    override fun hashCode(): Int = name.hashCode()
}

internal class KtUltraLightMethodForSourceDeclaration(
    delegate: LightMethodBuilder,
    declaration: KtDeclaration,
    support: KtUltraLightSupport,
    containingClass: KtUltraLightClass
) : KtUltraLightMethod(
    delegate,
    declaration,
    support,
    containingClass
) {
    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() = kotlinOrigin?.getKotlinType()

    override fun buildTypeParameterList(): PsiTypeParameterList {
        val origin = kotlinOrigin
        return if (origin is KtFunction || origin is KtProperty)
            buildTypeParameterList(origin as KtTypeParameterListOwner, this, support)
        else LightTypeParameterListBuilder(manager, language)
    }

    override fun computeDescriptor() = kotlinOrigin?.resolve() as? FunctionDescriptor
}

internal class KtUltraLightMethodForDescriptor(
    private val descriptor: FunctionDescriptor,
    delegate: LightMethodBuilder,
    closestDeclarationForOrigin: KtDeclaration?,
    support: KtUltraLightSupport,
    containingClass: KtUltraLightClass
) : KtUltraLightMethod(
    delegate,
    closestDeclarationForOrigin,
    support,
    containingClass
) {

    override fun buildTypeParameterList() = buildTypeParameterList(descriptor, this, support)

    override fun computeDescriptor() = descriptor

    override val kotlinTypeForNullabilityAnnotation: KotlinType?
        get() = descriptor.returnType

    override val givenAnnotations: List<KtLightAbstractAnnotation>
        get() = descriptor.obtainLightAnnotations(support, this)
}