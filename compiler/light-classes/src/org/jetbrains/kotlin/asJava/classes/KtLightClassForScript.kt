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

package org.jetbrains.kotlin.asJava.classes

import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.Key
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForScript
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtScript
import javax.swing.Icon

class KtLightClassForScript private constructor(
        val script: KtScript,
        private val lightClassDataCache: CachedValue<LightClassDataHolder.ForScript>
) : KtLazyLightClass(script.manager) {

    private val hashCode: Int = computeHashCode()

    private val packageFqName: FqName = script.fqName.parent()

    private val modifierList: PsiModifierList = LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC)

    private val implementsList: LightEmptyImplementsList = LightEmptyImplementsList(manager)

    private val _containingFile by lazyPub {
        FakeFileForLightClass(
                script.containingKtFile,
                lightClass = { this },
                stub = { lightClassDataCache.value.javaFileStub },
                packageFqName = packageFqName)
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

    override fun getImplementsList() = implementsList

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

    override fun copy() = KtLightClassForScript(script, lightClassDataCache)

    override val lightClassData = lightClassDataCache.value.findDataForScript(script.fqName)

    override fun getNavigationElement() = script

    override fun isEquivalentTo(another: PsiElement?): Boolean =
            another is PsiClass && Comparing.equal(another.qualifiedName, qualifiedName)

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by JetIconProvider")

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

        fun create(script: KtScript): KtLightClassForScript = KtLightClassForScript(script, getLightClassCachedValue(script))

        fun getLightClassCachedValue(script: KtScript): CachedValue<LightClassDataHolder.ForScript> {
            return script.getUserData(JAVA_API_STUB_FOR_SCRIPT) ?:
                   createCachedValueForScript(script).also { script.putUserData(JAVA_API_STUB_FOR_SCRIPT, it) }
        }

        private fun createCachedValueForScript(script: KtScript): CachedValue<LightClassDataHolder.ForScript> =
                CachedValuesManager.getManager(script.project).createCachedValue(LightClassDataProviderForScript(script), false)
    }

    override val originKind: LightClassOriginKind get() = LightClassOriginKind.SOURCE
}
