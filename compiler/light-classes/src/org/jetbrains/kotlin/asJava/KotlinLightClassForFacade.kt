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

package org.jetbrains.kotlin.asJava

import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import com.intellij.psi.util.PsiModificationTracker
import com.intellij.util.containers.SLRUCache
import org.jetbrains.annotations.NonNls
import org.jetbrains.kotlin.idea.JetLanguage
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.JetClassOrObject
import org.jetbrains.kotlin.psi.JetFile
import javax.swing.Icon

public class KotlinLightClassForFacade private constructor(
        manager: PsiManager,
        private val facadeClassFqName: FqName,
        private val searchScope: GlobalSearchScope,
        private val lightClassDataCache: CachedValue<KotlinFacadeLightClassData>,
        files: Collection<JetFile>
) : KotlinWrappingLightClass(manager), JetJavaMirrorMarker {

    private data class StubCacheKey(val fqName: FqName, val searchScope: GlobalSearchScope)

    public class PackageFacadeStubCache(private val project: Project) {
        private inner class PackageFacadeCacheData {
            val cache = object : SLRUCache<StubCacheKey, CachedValue<KotlinFacadeLightClassData>>(20, 30) {
                override fun createValue(key: StubCacheKey): CachedValue<KotlinFacadeLightClassData> {
                    val stubProvider = KotlinJavaFileStubProvider.createForPackageClass(project, key.fqName, key.searchScope)
                    return CachedValuesManager.getManager(project).createCachedValue<KotlinFacadeLightClassData>(stubProvider, /*trackValue = */false)
                }
            }
        }

        private val cachedValue: CachedValue<PackageFacadeCacheData> = CachedValuesManager.getManager(project).createCachedValue<PackageFacadeCacheData>(
                { CachedValueProvider.Result.create(PackageFacadeCacheData(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) },
                /*trackValue = */ false)

        public fun get(qualifiedName: FqName, searchScope: GlobalSearchScope): CachedValue<KotlinFacadeLightClassData> {
            synchronized (cachedValue) {
                return cachedValue.getValue().cache.get(StubCacheKey(qualifiedName, searchScope))
            }
        }

        companion object {
            public fun getInstance(project: Project): PackageFacadeStubCache {
                return ServiceManager.getService<PackageFacadeStubCache>(project, javaClass<PackageFacadeStubCache>())
            }
        }
    }

    public class FacadeStubCache(private val project: Project) {
        private inner class FacadeCacheData {
            val cache = object : SLRUCache<StubCacheKey, CachedValue<KotlinFacadeLightClassData>>(20, 30) {
                override fun createValue(key: StubCacheKey): CachedValue<KotlinFacadeLightClassData> {
                    val stubProvider = KotlinJavaFileStubProvider.createForFacadeClass(project, key.fqName, key.searchScope)
                    return CachedValuesManager.getManager(project).createCachedValue<KotlinFacadeLightClassData>(stubProvider, /*trackValue = */false)
                }
            }
        }

        private val cachedValue: CachedValue<FacadeCacheData> = CachedValuesManager.getManager(project).createCachedValue<FacadeCacheData>(
                { CachedValueProvider.Result.create(FacadeCacheData(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT) },
                /*trackValue = */ false)

        public fun get(qualifiedName: FqName, searchScope: GlobalSearchScope): CachedValue<KotlinFacadeLightClassData> {
            synchronized (cachedValue) {
                return cachedValue.getValue().cache.get(StubCacheKey(qualifiedName, searchScope))
            }
        }

        companion object {
            public fun getInstance(project: Project): FacadeStubCache {
                return ServiceManager.getService<FacadeStubCache>(project, javaClass<FacadeStubCache>())
            }
        }
    }

    public val files: Collection<JetFile> = files.toSet() // needed for hashCode

    private val hashCode: Int =
            computeHashCode()

    private val packageFqName: FqName =
            facadeClassFqName.parent()

    private val modifierList: PsiModifierList =
            LightModifierList(manager, JetLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)

    private val implementsList: LightEmptyImplementsList =
            LightEmptyImplementsList(manager)

    private val packageClsFile: ClsFileImpl = KotlinJavaFileStubProvider.createFakeClsFile(manager.getProject(), packageFqName, files) {
        (getDelegate().getContainingFile() as ClsFileImpl).getStub()
    }

    override fun getOrigin(): JetClassOrObject? = null

    override fun getFqName(): FqName = facadeClassFqName

    override fun getModifierList() = modifierList

    override fun hasModifierProperty(NonNls name: String) = modifierList.hasModifierProperty(name)

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

    // TODO: Find a way to return just Object
    override fun getExtendsList() = super<KotlinWrappingLightClass>.getExtendsList()

    // TODO see getExtendsList()
    override fun getExtendsListTypes() = super<KotlinWrappingLightClass>.getExtendsListTypes()

    // TODO see getExtendsList()
    override fun getSuperClass(): PsiClass? = super<KotlinWrappingLightClass>.getSuperClass()

    // TODO see getExtendsList()
    override fun getSupers(): Array<PsiClass> = super<KotlinWrappingLightClass>.getSupers()

    // TODO see getExtendsList()
    override fun getSuperTypes() = super<KotlinWrappingLightClass>.getSuperTypes()

    override fun getInterfaces() = PsiClass.EMPTY_ARRAY

    override fun getInnerClasses() = PsiClass.EMPTY_ARRAY

    override fun getOwnInnerClasses(): List<PsiClass> = listOf()

    override fun getAllInnerClasses() = PsiClass.EMPTY_ARRAY

    override fun getInitializers() = PsiClassInitializer.EMPTY_ARRAY

    override fun findInnerClassByName(NonNls name: String, checkBases: Boolean) = null

    override fun getName() = facadeClassFqName.shortName().asString()

    override fun getQualifiedName() = facadeClassFqName.asString()

    override fun isValid() = files.all { it.isValid() }

    override fun copy() = KotlinLightClassForFacade(getManager(), facadeClassFqName, searchScope, lightClassDataCache, files)

    override fun getDelegate(): PsiClass {
        val psiClass = LightClassUtil.findClass(facadeClassFqName, lightClassDataCache.getValue().javaFileStub)
                       ?: throw IllegalStateException("Facade class $facadeClassFqName not found")
        return psiClass
    }

    override fun getNavigationElement() = files.iterator().next()

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return another is PsiClass && Comparing.equal(another.getQualifiedName(), getQualifiedName())
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

        val lightClass = other as KotlinLightClassForFacade
        if (this === other) return true

        if (this.hashCode != lightClass.hashCode) return false
        if (getManager() != lightClass.getManager()) return false
        if (files != lightClass.files) return false
        if (facadeClassFqName != lightClass.facadeClassFqName) return false

        return true
    }

    override fun toString(): String {
        try {
            return javaClass<KotlinLightClassForFacade>().getSimpleName() + ":" + getQualifiedName()
        }
        catch (e: Throwable) {
            return javaClass<KotlinLightClassForFacade>().getSimpleName() + ":" + e.toString()
        }
    }

    companion object Factory {
        public fun createForPackageFacade(
                manager: PsiManager,
                packageFqName: FqName,
                searchScope: GlobalSearchScope,
                files: Collection<JetFile> // this is redundant, but computing it multiple times is costly
        ): KotlinLightClassForFacade? {
            if (files.any { LightClassUtil.belongsToKotlinBuiltIns(it) }) {
                return null
            }

            assert(files.isNotEmpty()) { "No files for package $packageFqName" }

            val packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName)
            val lightClassDataCache = PackageFacadeStubCache.getInstance(manager.project).get(packageFqName, searchScope)
            return KotlinLightClassForFacade(manager, packageClassFqName, searchScope, lightClassDataCache, files)
        }

        public fun createForFacade(
                manager: PsiManager,
                facadeClassFqName: FqName,
                searchScope: GlobalSearchScope,
                files: Collection<JetFile>
        ): KotlinLightClassForFacade? {
            if (files.any { LightClassUtil.belongsToKotlinBuiltIns(it) }) {
                return null
            }

            assert(files.isNotEmpty()) { "No files for facade $facadeClassFqName" }

            val lightClassDataCache = FacadeStubCache.getInstance(manager.project).get(facadeClassFqName, searchScope)
            return KotlinLightClassForFacade(manager, facadeClassFqName, searchScope, lightClassDataCache, files)
        }
    }
}
