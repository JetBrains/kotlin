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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.psi.*
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.fileClasses.JvmFileClassUtil
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.idea.KotlinLanguage
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils
import org.jetbrains.kotlin.load.kotlin.PackagePartClassUtils.fileHasTopLevelCallables
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtPsiFactory
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.psiUtil.siblings
import javax.swing.Icon

class KtLightClassForFacade private constructor(
        manager: PsiManager,
        private val facadeClassFqName: FqName,
        private val lightClassDataCache: CachedValue<KotlinFacadeLightClassData>,
        files: Collection<KtFile>
) : KtWrappingLightClass(manager), KtJavaMirrorMarker {

    private data class StubCacheKey(val fqName: FqName, val searchScope: GlobalSearchScope)

    class FacadeStubCache(private val project: Project) {
        private inner class FacadeCacheData {
            val cache = object : SLRUCache<StubCacheKey, CachedValue<KotlinFacadeLightClassData>>(20, 30) {
                override fun createValue(key: StubCacheKey): CachedValue<KotlinFacadeLightClassData> {
                    val stubProvider = LightClassDataProviderForFileFacade.ByProjectSource(project, key.fqName, key.searchScope)
                    return CachedValuesManager.getManager(project).createCachedValue<KotlinFacadeLightClassData>(stubProvider, /*trackValue = */false)
                }
            }
        }

        private val cachedValue: CachedValue<FacadeCacheData> = CachedValuesManager.getManager(project).createCachedValue<FacadeCacheData>(
                { CachedValueProvider.Result.create(FacadeCacheData(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) },
                /*trackValue = */ false)

        operator fun get(qualifiedName: FqName, searchScope: GlobalSearchScope): CachedValue<KotlinFacadeLightClassData> {
            synchronized (cachedValue) {
                return cachedValue.value.cache.get(StubCacheKey(qualifiedName, searchScope))
            }
        }

        companion object {
            fun getInstance(project: Project): FacadeStubCache {
                return ServiceManager.getService<FacadeStubCache>(project, FacadeStubCache::class.java)
            }
        }
    }

    val files: Collection<KtFile> = files.toSet() // needed for hashCode

    private val hashCode: Int =
            computeHashCode()

    private val packageFqName: FqName =
            facadeClassFqName.parent()

    private val modifierList: PsiModifierList =
            LightModifierList(manager, KotlinLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList =
            LightEmptyImplementsList(manager)

    private val packageClsFile = FakeFileForLightClass(
            files.first(),
            lightClass = { this },
            stub = { lightClassDataCache.value.javaFileStub },
            packageFqName = packageFqName
    )

    override val kotlinOrigin: KtClassOrObject? get() = null

    override fun getFqName(): FqName = facadeClassFqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(@NonNls name: String) = modifierList.hasModifierProperty(name)

    override fun isDeprecated() = false

    override fun isInterface() = false

    override fun isAnnotationType() = false

    override fun isEnum() = false

    override fun getContainingClass() = null

    override fun getContainingFile() = packageClsFile

    override fun hasTypeParameters() = false

    override fun getTypeParameters() = PsiTypeParameter.EMPTY_ARRAY

    override fun getTypeParameterList() = null

    override fun getDocComment() = null

    override fun getImplementsList() = implementsList

    override fun getImplementsListTypes() = PsiClassType.EMPTY_ARRAY

    override fun getInterfaces() = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses() = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses() = PsiClass.EMPTY_ARRAY

    override fun getInitializers() = PsiClassInitializer.EMPTY_ARRAY

    override fun findInnerClassByName(@NonNls name: String, checkBases: Boolean) = null

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
                }
                else {
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

    override fun isValid() = files.all { it.isValid && fileHasTopLevelCallables(it) && facadeClassFqName == it.javaFileFacadeFqName }

    override fun copy() = KtLightClassForFacade(getManager(), facadeClassFqName, lightClassDataCache, files)

    override val clsDelegate: PsiClass
        get() {
            val psiClass = LightClassUtil.findClass(facadeClassFqName, lightClassDataCache.value.javaFileStub)
                           ?: throw IllegalStateException("Facade class $facadeClassFqName not found")
            return psiClass
        }

    override fun getNavigationElement() = files.iterator().next()

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return another is PsiClass && Comparing.equal(another.qualifiedName, getQualifiedName())
    }

    override fun getElementIcon(flags: Int): Icon? = throw UnsupportedOperationException("This should be done by JetIconProvider")

    override fun hashCode() = hashCode

    private fun computeHashCode(): Int {
        var result = getManager().hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + facadeClassFqName.hashCode()
        return result
    }

    override fun equals(other: Any?): Boolean {
        if (other == null || javaClass != other.javaClass) {
            return false
        }

        val lightClass = other as KtLightClassForFacade
        if (this === other) return true

        if (this.hashCode != lightClass.hashCode) return false
        if (getManager() != lightClass.getManager()) return false
        if (files != lightClass.files) return false
        if (facadeClassFqName != lightClass.facadeClassFqName) return false

        return true
    }

    override fun toString() = "${KtLightClassForFacade::class.java.simpleName}:$facadeClassFqName"

    companion object Factory {
        fun createForFacade(
                manager: PsiManager,
                facadeClassFqName: FqName,
                searchScope: GlobalSearchScope,
                files: Collection<KtFile>
        ): KtLightClassForFacade {
            assert(files.isNotEmpty()) { "No files for facade $facadeClassFqName" }

            val lightClassDataCache = FacadeStubCache.getInstance(manager.project).get(facadeClassFqName, searchScope)
            return KtLightClassForFacade(manager, facadeClassFqName, lightClassDataCache, files)
        }

        fun createForSyntheticFile(
                manager: PsiManager,
                facadeClassFqName: FqName,
                file: KtFile
        ): KtLightClassForFacade {
            // TODO: refactor, using cached value doesn't make sense for this case
            val cachedValue = CachedValuesManager.getManager(manager.project).
                    createCachedValue<KotlinFacadeLightClassData>(
                            LightClassDataProviderForFileFacade.ByFile(manager.project, facadeClassFqName, file), /*trackValue = */false
                    )
            return KtLightClassForFacade(manager, facadeClassFqName, cachedValue, listOf(file))
        }
    }
}
