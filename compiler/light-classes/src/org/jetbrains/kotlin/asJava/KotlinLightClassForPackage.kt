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

import com.google.common.collect.Sets
import com.intellij.openapi.components.ServiceManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Comparing
import com.intellij.psi.*
import com.intellij.psi.impl.compiled.ClsFileImpl
import com.intellij.psi.impl.light.LightEmptyImplementsList
import com.intellij.psi.impl.light.LightModifierList
import com.intellij.psi.javadoc.PsiDocComment
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiClassHolderFileStub
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

import javax.swing.*

public class KotlinLightClassForPackage private(manager: PsiManager, private val packageFqName: FqName, private val searchScope: GlobalSearchScope, files: Collection<JetFile>) : KotlinWrappingLightClass(manager), JetJavaMirrorMarker {

    public class FileStubCache(private val project: Project) {

        private class Key private(private val fqName: FqName, private val searchScope: GlobalSearchScope) {

            override fun equals(o: Any?): Boolean {
                if (this == o) return true
                if (o == null || javaClass != o.javaClass) return false

                val key = o as Key

                if (fqName != key.fqName) return false
                if (searchScope != key.searchScope) return false

                return true
            }

            override fun hashCode(): Int {
                var result = fqName.hashCode()
                result = 31 * result + searchScope.hashCode()
                return result
            }
        }

        private inner class CacheData {

            private val cache = object : SLRUCache<Key, CachedValue<KotlinPackageLightClassData>>(20, 30) {
                override fun createValue(key: Key): CachedValue<KotlinPackageLightClassData> {
                    val stubProvider = KotlinJavaFileStubProvider.createForPackageClass(project, key.fqName, key.searchScope)
                    return CachedValuesManager.getManager(project).createCachedValue<KotlinPackageLightClassData>(stubProvider, /*trackValue = */false)
                }
            }
        }

        private val cachedValue: CachedValue<CacheData>

        {
            this.cachedValue = CachedValuesManager.getManager(project).createCachedValue<CacheData>(object : CachedValueProvider<CacheData> {
                override fun compute(): CachedValueProvider.Result<CacheData>? {
                    return CachedValueProvider.Result.create<CacheData>(CacheData(), PsiModificationTracker.OUT_OF_CODE_BLOCK_MODIFICATION_COUNT)
                }
            }, /*trackValue = */ false)
        }

        public fun get(qualifiedName: FqName, searchScope: GlobalSearchScope): CachedValue<KotlinPackageLightClassData> {
            synchronized (cachedValue) {
                return cachedValue.getValue().cache.get(Key(qualifiedName, searchScope))
            }
        }

        class object {

            public fun getInstance(project: Project): FileStubCache {
                return ServiceManager.getService<FileStubCache>(project, javaClass<FileStubCache>())
            }
        }

    }

    private val packageClassFqName: FqName // derived from packageFqName
    public val files: Collection<JetFile>
    private val hashCode: Int
    private val lightClassDataCache: CachedValue<KotlinPackageLightClassData>
    private val modifierList: PsiModifierList
    private val implementsList: LightEmptyImplementsList
    private val packageClsFile: ClsFileImpl

    {
        this.modifierList = LightModifierList(manager, JetLanguage.INSTANCE, PsiModifier.PUBLIC, PsiModifier.FINAL)
        this.implementsList = LightEmptyImplementsList(manager)
        this.packageClassFqName = PackageClassUtils.getPackageClassFqName(packageFqName)
        assert(!files.isEmpty()) { "No files for package " + packageFqName }
        this.files = Sets.newHashSet<JetFile>(files) // needed for hashCode
        this.hashCode = computeHashCode()
        this.lightClassDataCache = FileStubCache.getInstance(getProject()).get(packageFqName, searchScope)

        val virtualFile = KotlinJavaFileStubProvider.getRepresentativeVirtualFile(files)
        packageClsFile = object : ClsFileImpl(ClassFileViewProvider(PsiManager.getInstance(getProject()), virtualFile)) {
            override fun getStub(): PsiClassHolderFileStub<PsiFile> {
                return (getDelegate().getContainingFile() as ClsFileImpl).getStub()
            }

            override fun getPackageName(): String {
                return this@KotlinLightClassForPackage.packageFqName.asString()
            }
        }
        packageClsFile.setPhysical(false)
    }

    override fun getOrigin(): JetClassOrObject? {
        return null
    }

    override fun getModifierList(): PsiModifierList? {
        return modifierList
    }

    override fun hasModifierProperty(NonNls name: String): Boolean {
        return modifierList.hasModifierProperty(name)
    }

    override fun isDeprecated(): Boolean {
        return false
    }

    override fun isInterface(): Boolean {
        return false
    }

    override fun isAnnotationType(): Boolean {
        return false
    }

    override fun isEnum(): Boolean {
        return false
    }

    override fun getContainingClass(): PsiClass? {
        return null
    }

    override fun getContainingFile(): PsiFile {
        return packageClsFile
    }

    override fun hasTypeParameters(): Boolean {
        return false
    }

    override fun getTypeParameters(): Array<PsiTypeParameter> {
        return PsiTypeParameter.EMPTY_ARRAY
    }

    override fun getTypeParameterList(): PsiTypeParameterList? {
        return null
    }

    override fun getDocComment(): PsiDocComment? {
        return null
    }

    override fun getImplementsList(): PsiReferenceList? {
        return implementsList
    }

    override fun getImplementsListTypes(): Array<PsiClassType> {
        return PsiClassType.EMPTY_ARRAY
    }

    override fun getExtendsList(): PsiReferenceList? {
        // TODO: Find a way to return just Object
        return super.getExtendsList()
    }

    override fun getExtendsListTypes(): Array<PsiClassType> {
        // TODO see getExtendsList()
        return super.getExtendsListTypes()
    }

    override fun getSuperClass(): PsiClass? {
        // TODO see getExtendsList()
        return super.getSuperClass()
    }

    override fun getSupers(): Array<PsiClass> {
        // TODO see getExtendsList()
        return super.getSupers()
    }

    override fun getSuperTypes(): Array<PsiClassType> {
        // TODO see getExtendsList()
        return super.getSuperTypes()
    }

    override fun getInterfaces(): Array<PsiClass> {
        return PsiClass.EMPTY_ARRAY
    }

    override fun getInnerClasses(): Array<PsiClass> {
        return PsiClass.EMPTY_ARRAY
    }

    override fun getOwnInnerClasses(): List<PsiClass> {
        return listOf()
    }

    override fun getAllInnerClasses(): Array<PsiClass> {
        return PsiClass.EMPTY_ARRAY
    }

    override fun getInitializers(): Array<PsiClassInitializer> {
        return PsiClassInitializer.EMPTY_ARRAY
    }

    override fun findInnerClassByName(NonNls name: String, checkBases: Boolean): PsiClass? {
        return null
    }

    override fun getFqName(): FqName {
        return packageClassFqName
    }

    override fun getName(): String? {
        return packageClassFqName.shortName().asString()
    }

    override fun getQualifiedName(): String? {
        return packageClassFqName.asString()
    }

    override fun isValid(): Boolean {
        return allValid(files)
    }

    override fun copy(): PsiElement {
        return KotlinLightClassForPackage(getManager(), packageFqName, searchScope, files)
    }

    override fun getDelegate(): PsiClass {
        val psiClass = LightClassUtil.findClass(packageClassFqName, lightClassDataCache.getValue().javaFileStub)
        if (psiClass == null) {
            throw IllegalStateException("Package class was not found " + packageFqName)
        }
        return psiClass
    }

    override fun getNavigationElement(): PsiElement {
        return files.iterator().next()
    }

    override fun isEquivalentTo(another: PsiElement?): Boolean {
        return another is PsiClass && Comparing.equal((another as PsiClass).getQualifiedName(), getQualifiedName())
    }

    override fun getElementIcon(flags: Int): Icon? {
        throw UnsupportedOperationException("This should be done byt JetIconProvider")
    }

    override fun hashCode(): Int {
        return hashCode
    }

    private fun computeHashCode(): Int {
        var result = getManager().hashCode()
        result = 31 * result + files.hashCode()
        result = 31 * result + packageFqName.hashCode()
        return result
    }

    override fun equals(obj: Any?): Boolean {
        if (this == obj) return true
        if (obj == null || javaClass != obj.javaClass) {
            return false
        }

        val lightClass = obj as KotlinLightClassForPackage

        if (this.hashCode != lightClass.hashCode) return false
        if (getManager() != lightClass.getManager()) return false
        if (files != lightClass.files) return false
        if (packageFqName != lightClass.packageFqName) return false

        return true
    }

    override fun toString(): String {
        try {
            return javaClass<KotlinLightClassForPackage>().getSimpleName() + ":" + getQualifiedName()
        }
        catch (e: Throwable) {
            return javaClass<KotlinLightClassForPackage>().getSimpleName() + ":" + e.toString()
        }

    }

    class object {

        public fun create(manager: PsiManager, qualifiedName: FqName, searchScope: GlobalSearchScope, files: Collection<JetFile> // this is redundant, but computing it multiple times is costly
        ): KotlinLightClassForPackage? {
            for (file in files) {
                if (LightClassUtil.belongsToKotlinBuiltIns(file)) return null
            }
            return KotlinLightClassForPackage(manager, qualifiedName, searchScope, files)
        }

        private fun allValid(files: Collection<JetFile>): Boolean {
            for (file in files) {
                if (!file.isValid()) return false
            }
            return true
        }
    }
}//NOTE: this is only needed to compute plugin module info
