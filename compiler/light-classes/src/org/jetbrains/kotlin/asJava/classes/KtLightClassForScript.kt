/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.util.IncorrectOperationException
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.analyzer.KotlinModificationTrackerService
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassData
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForScript
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.load.kotlin.TypeMappingMode
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCodeFragment
import org.jetbrains.kotlin.psi.KtScript
import javax.swing.Icon

open class KtLightClassForScript(val script: KtScript) : KtLazyLightClass(script.manager) {

    protected open fun getLightClassDataHolder(): LightClassDataHolder.ForScript =
        getLightClassCachedValue(script).value

    override val lightClassData: LightClassData
        get() = getLightClassDataHolder().findDataForScript(script.fqName)

    protected open val javaFileStub: PsiJavaFileStub?
        get() = getLightClassDataHolder().javaFileStub

    private val hashCode: Int = computeHashCode()

    private val packageFqName: FqName = script.fqName.parent()

    private val modifierList: PsiModifierList = LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC)

    private val scriptImplementsList: LightEmptyImplementsList = LightEmptyImplementsList(manager)

    private val scriptExtendsList: PsiReferenceList by lazyPub {
        KotlinLightReferenceListBuilder(manager, PsiReferenceList.Role.EXTENDS_LIST).also {
            it.addReference("kotlin.script.templates.standard.ScriptTemplateWithArgs")
        }
    }

    private val _containingFile by lazyPub {
        FakeFileForLightClass(
            script.containingKtFile,
            lightClass = { this },
            stub = { javaFileStub },
            packageFqName = packageFqName,
        )
    }

    override val kotlinOrigin: KtClassOrObject? get() = null

    val fqName: FqName = script.fqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass() = null

    override fun getContainingFile() = _containingFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters(): Array<PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList() = null

    override fun getDocComment() = null

    override fun getImplementsList(): PsiReferenceList = scriptImplementsList

    override fun getExtendsList(): PsiReferenceList = scriptExtendsList

    override fun getImplementsListTypes(): Array<PsiClassType> = PsiClassType.EMPTY_ARRAY

    override fun getInterfaces(): Array<PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> {
        return script.declarations.filterIsInstance<KtClassOrObject>()
            // workaround for ClassInnerStuffCache not supporting classes with null names, see KT-13927
            // inner classes with null names can't be searched for and can't be used from java anyway
            // we can't prohibit creating light classes with null names either since they can contain members
            .filter { it.name != null }
            .mapNotNull { KtLightClassForSourceDeclaration.create(it) }
    }

    override fun getInitializers(): Array<PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun getName() = script.fqName.shortName().asString()

    override fun getQualifiedName() = script.fqName.asString()

    override fun isValid() = script.isValid

    override fun copy() = KtLightClassForScript(script)

    override fun getNavigationElement() = script

    override fun isEquivalentTo(another: PsiElement?): Boolean =
        another is PsiClass && Comparing.equal(another.qualifiedName, qualifiedName)

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by JetIconProvider")

    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE

    override fun getLBrace(): PsiElement? = null

    override fun getRBrace(): PsiElement? = null

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    override fun setName(name: String): PsiElement? = throw IncorrectOperationException()

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> {
        return superClass?.let { arrayOf(it) } ?: arrayOf()
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return arrayOf(PsiType.getJavaLangObject(manager, resolveScope))
    }

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun getParent(): PsiElement = containingFile

    override fun getScope(): PsiElement = parent

    override fun hashCode() = hashCode

    private fun computeHashCode(): Int {
        var result = manager.hashCode()
        result = 31 * result + script.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || this::class.java != other::class.java) {
            return false
        }

        val lightClass = other as KtLightClassForScript
        if (this === other) return true

        if (this.hashCode != lightClass.hashCode) return false
        if (manager != lightClass.manager) return false
        if (script != lightClass.script) return false

        return true
    }

    override fun toString() = "${KtLightClassForScript::class.java.simpleName}:${script.fqName}"

    companion object {

        private val JAVA_API_STUB_FOR_SCRIPT = Key.create<CachedValue<LightClassDataHolder.ForScript>>("JAVA_API_STUB_FOR_SCRIPT")

        fun create(script: KtScript): KtLightClassForScript? =
            CachedValuesManager.getCachedValue(script) {
                CachedValueProvider.Result
                    .create(
                        createNoCache(script, KtUltraLightSupport.forceUsingOldLightClasses),
                        KotlinModificationTrackerService.getInstance(script.project).outOfBlockModificationTracker,
                    )
            }

        fun createNoCache(script: KtScript, forceUsingOldLightClasses: Boolean): KtLightClassForScript? {
            val containingFile = script.containingFile
            if (containingFile is KtCodeFragment) {
                // Avoid building light classes for code fragments
                return null
            }

            if (!forceUsingOldLightClasses && Registry.`is`("kotlin.use.ultra.light.classes", true)) {
                return LightClassGenerationSupport.getInstance(script.project).createUltraLightClassForScript(script)
                    ?: error("UL class cannot be created for script")
            }

            return KtLightClassForScript(script)
        }

        fun getLightClassCachedValue(script: KtScript): CachedValue<LightClassDataHolder.ForScript> {
            return script.getUserData(JAVA_API_STUB_FOR_SCRIPT) ?: createCachedValueForScript(script).also {
                script.putUserData(
                    JAVA_API_STUB_FOR_SCRIPT,
                    it,
                )
            }
        }

        private fun createCachedValueForScript(script: KtScript): CachedValue<LightClassDataHolder.ForScript> =
            CachedValuesManager.getManager(script.project).createCachedValue(LightClassDataProviderForScript(script), false)
    }
}
