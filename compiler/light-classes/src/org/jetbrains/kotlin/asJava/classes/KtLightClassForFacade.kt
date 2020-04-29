/*
 * Copyright 2010-2016 JetBrains s.r.o.
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

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.*
import com.intellij.psi.impl.PsiSuperMethodImplUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValuesManager
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.asJava.KotlinAsJavaSupport
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.builder.LightClassDataHolder
import org.jetbrains.kotlin.asJava.builder.LightClassDataProviderForFileFacade
import org.jetbrains.kotlin.asJava.elements.FakeFileForLightClass
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.java.structure.LightClassOriginKind
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import javax.swing.Icon

open class KtLightClassForFacade constructor(
    manager: PsiManager,
    protected val facadeClassFqName: FqName,
    myLightClassDataCache: CachedValue<LightClassDataHolder.ForFacade>,
    files: Collection<KtFile>
) : KtLazyLightClass(manager) {

    protected open val lightClassDataCache: CachedValue<LightClassDataHolder.ForFacade> = myLightClassDataCache

    protected open val javaFileStub: PsiJavaFileStub?
        get() = lightClassDataCache.value.javaFileStub

    override val lightClassData
        get() = lightClassDataCache.value.findDataForFacade(facadeClassFqName)

    val files: Collection<KtFile> = files.toSet()

    private val firstFileInFacade by lazyPub { files.iterator().next() }

    private val hashCode: Int =
        computeHashCode()

    private val packageFqName: FqName =
        facadeClassFqName.parent()

    private val modifierList: PsiModifierList =
        LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList =
        LightEmptyImplementsList(manager)

    private val packageClsFile = FakeFileForLightClass(
        firstFileInFacade,
        lightClass = { this },
        stub = { javaFileStub },
        packageFqName = packageFqName
    )

    override fun getParent(): PsiElement = containingFile

    override val kotlinOrigin: KtClassOrObject? get() = null

    val fqName: FqName
        get() = facadeClassFqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

    override fun getExtendsList(): PsiReferenceList? = null

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass(): PsiClass? = null

    override fun getContainingFile() = packageClsFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters(): Array<out PsiTypeParameter> = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList(): PsiTypeParameterList? = null

    override fun getDocComment(): Nothing? = null

    override fun getImplementsList() = implementsList

    override fun getImplementsListTypes(): Array<out PsiClassType> = PsiClassType.EMPTY_ARRAY

    override fun getInterfaces(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses(): Array<out PsiClass> = PsiClass.EMPTY_ARRAY

    override fun getInitializers(): Array<out PsiClassInitializer> = PsiClassInitializer.EMPTY_ARRAY

    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean): PsiClass? = null

    override fun isInheritorDeep(baseClass: PsiClass?, classToByPass: PsiClass?): Boolean = false

    override fun getLBrace(): PsiElement? = null

    override fun getRBrace(): PsiElement? = null

    override fun getName() = facadeClassFqName.shortName().asString()

    override fun setName(name: String): PsiElement? {
        for (file in files) {
            val jvmNameEntry = JvmFileClassUtil.findAnnotationEntryOnFileNoResolve(file, JvmFileClassUtil.JVM_NAME_SHORT)

            if (PackagePartClassUtils.getFilePartShortName(file.name) == name) {
                jvmNameEntry?.delete()
                continue
            }

            if (jvmNameEntry == null) {
                val newFileName = PackagePartClassUtils.getFileNameByFacadeName(name)
                val facadeDir = file.parent
                if (newFileName != null && facadeDir != null && facadeDir.findFile(newFileName) == null) {
                    file.name = newFileName
                    continue
                }

                val psiFactory = KtPsiFactory(this)
                val annotationText = "${JvmFileClassUtil.JVM_NAME_SHORT}(\"$name\")"
                val newFileAnnotationList = psiFactory.createFileAnnotationListWithAnnotation(annotationText)
                val annotationList = file.fileAnnotationList
                if (annotationList != null) {
                    annotationList.add(newFileAnnotationList.annotationEntries.first())
                } else {
                    val anchor = file.firstChild.siblings().firstOrNull { it !is PsiWhiteSpace && it !is PsiComment }
                    file.addBefore(newFileAnnotationList, anchor)
                }
                continue
            }

            val jvmNameExpression = jvmNameEntry.valueArguments.firstOrNull()?.getArgumentExpression() as? KtStringTemplateExpression
                ?: continue
            ElementManipulators.handleContentChange(jvmNameExpression, name)
        }

        return this
    }

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun getNameIdentifier(): PsiIdentifier? = null

    override fun isValid() = files.all { it.isValid && it.hasTopLevelCallables() && facadeClassFqName == it.javaFileFacadeFqName }

    override fun copy() = KtLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files)

    override fun getNavigationElement() = firstFileInFacade

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return another is KtLightClassForFacade && Comparing.equal(another.qualifiedName, qualifiedName)
    }

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by JetIconProvider")

    override fun isInheritor(baseClass: PsiClass, checkDeep: Boolean): Boolean {
        return baseClass.qualifiedName == CommonClassNames.JAVA_LANG_OBJECT
    }

    override fun getSuperClass(): PsiClass? {
        return JavaPsiFacade.getInstance(project).findClass(CommonClassNames.JAVA_LANG_OBJECT, resolveScope)
    }

    override fun getSupers(): Array<PsiClass> {
        return superClass?.let { arrayOf(it) } ?: arrayOf()
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        return arrayOf(PsiType.getJavaLangObject(manager, resolveScope))
    }

    override fun hashCode() = hashCode

    private fun computeHashCode(): Int {
        var result = manager.hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + facadeClassFqName.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || this::class.java != other::class.java) {
            return false
        }

        val lightClass = other as KtLightClassForFacade
        if (this === other) return true

        if (this.hashCode != lightClass.hashCode) return false
        if (manager != lightClass.manager) return false
        if (files != lightClass.files) return false
        if (facadeClassFqName != lightClass.facadeClassFqName) return false

        return true
    }

    override fun toString() = "${KtLightClassForFacade::class.java.simpleName}:$facadeClassFqName"

    override val originKind: LightClassOriginKind
        get() = LightClassOriginKind.SOURCE

    override fun getText() = firstFileInFacade.text ?: ""

    override fun getTextRange(): TextRange = firstFileInFacade.textRange ?: TextRange.EMPTY_RANGE

    override fun getTextOffset() = firstFileInFacade.textOffset

    override fun getStartOffsetInParent() = firstFileInFacade.startOffsetInParent

    override fun isWritable() = files.all { it.isWritable }

    override fun getVisibleSignatures(): MutableCollection<HierarchicalMethodSignature> = PsiSuperMethodImplUtil.getVisibleSignatures(this)

    companion object {

        fun createForFacadeNoCache(fqName: FqName, searchScope: GlobalSearchScope, project: Project): KtLightClassForFacade? {

            val sources = KotlinAsJavaSupport.getInstance(project).findFilesForFacade(fqName, searchScope)
                .filterNot { it.isCompiled }

            if (sources.isEmpty()) return null

            val stubProvider = LightClassDataProviderForFileFacade.ByProjectSource(project, fqName, searchScope)
            val stubValue = CachedValuesManager.getManager(project)
                .createCachedValue(stubProvider, false)

            val manager = PsiManager.getInstance(project)

            return if (!KtUltraLightSupport.forceUsingOldLightClasses && Registry.`is`("kotlin.use.ultra.light.classes", true))
                LightClassGenerationSupport.getInstance(project).createUltraLightClassForFacade(manager, fqName, stubValue, sources)
                    ?: error { "Unable to create UL class for facade" }
            else KtLightClassForFacade(manager, fqName, stubValue, sources)
        }

        fun createForFacade(
            manager: PsiManager,
            facadeClassFqName: FqName,
            searchScope: GlobalSearchScope
        ): KtLightClassForFacade? {
            return FacadeCache.getInstance(manager.project)[facadeClassFqName, searchScope]
        }

        fun createForSyntheticFile(
            manager: PsiManager,
            facadeClassFqName: FqName,
            file: KtFile
        ): KtLightClassForFacade {
            // TODO: refactor, using cached value doesn't make sense for this case
            val cachedValue = CachedValuesManager.getManager(manager.project).createCachedValue(
                LightClassDataProviderForFileFacade.ByFile(manager.project, facadeClassFqName, file), false
            )
            return KtLightClassForFacade(manager, facadeClassFqName, cachedValue, listOf(file))
        }
    }
}
