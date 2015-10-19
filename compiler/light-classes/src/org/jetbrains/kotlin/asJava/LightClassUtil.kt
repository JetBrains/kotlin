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

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.util.Comparing
import com.intellij.openapi.util.io.FileUtil
import com.intellij.openapi.vfs.StandardFileSystems
import com.intellij.psi.*
import com.intellij.psi.impl.java.stubs.PsiClassStub
import com.intellij.psi.search.GlobalSearchScope
import com.intellij.psi.stubs.PsiFileStub
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.util.PsiTreeUtil
import com.intellij.util.PathUtil
import com.intellij.util.SmartList
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.fileClasses.javaFileFacadeFqName
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.load.kotlin.PackageClassUtils
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.getNonStrictParentOfType
import org.jetbrains.kotlin.utils.KotlinVfsUtil
import org.jetbrains.kotlin.utils.rethrow
import java.io.File
import java.net.MalformedURLException
import java.net.URL

public object LightClassUtil {
    private val LOG = Logger.getInstance(LightClassUtil::class.java)

    public val BUILT_INS_SRC_DIR: File = File("core/builtins/native", KotlinBuiltIns.BUILT_INS_PACKAGE_NAME.asString())

    public val builtInsDirUrl: URL by lazy { computeBuiltInsDir() }

    /**
     * Checks whether the given file is loaded from the location where Kotlin's built-in classes are defined.
     * As of today, this is core/builtins/native/kotlin directory and files such as Any.kt, Nothing.kt etc.

     * Used to skip JetLightClass creation for built-ins, because built-in classes have no Java counterparts
     */
    public fun belongsToKotlinBuiltIns(file: KtFile): Boolean {
        val virtualFile = file.virtualFile
        if (virtualFile != null) {
            val parent = virtualFile.parent
            if (parent != null) {
                try {
                    val jetVfsPathUrl = KotlinVfsUtil.convertFromUrl(builtInsDirUrl)
                    val fileDirVfsUrl = parent.url
                    if (jetVfsPathUrl == fileDirVfsUrl) {
                        return true
                    }
                }
                catch (e: MalformedURLException) {
                    LOG.error(e)
                }

            }
        }

        // We deliberately return false on error: who knows what weird URLs we might come across out there
        // it would be a pity if no light classes would be created in such cases
        return false
    }

    private fun computeBuiltInsDir(): URL {
        val builtInFilePath = "/" + KotlinBuiltIns.BUILT_INS_PACKAGE_NAME + "/Library.kt"

        val url = KotlinBuiltIns::class.java.getResource(builtInFilePath)

        if (url == null) {
            if (ApplicationManager.getApplication().isUnitTestMode) {
                // HACK: Temp code. Get built-in files from the sources when running from test.
                try {
                    return URL(StandardFileSystems.FILE_PROTOCOL, "",
                               FileUtil.toSystemIndependentName(BUILT_INS_SRC_DIR.absolutePath))
                }
                catch (e: MalformedURLException) {
                    throw rethrow(e)
                }

            }

            throw IllegalStateException("Built-ins file wasn't found at url: " + builtInFilePath)
        }

        try {
            return URL(url.protocol, url.host, PathUtil.getParentPath(url.file))
        }
        catch (e: MalformedURLException) {
            throw AssertionError(e)
        }

    }

    fun findClass(fqn: FqName, stub: StubElement<*>): PsiClass? {
        if (stub is PsiClassStub<*> && Comparing.equal(fqn.asString(), stub.qualifiedName)) {
            return stub.getPsi()
        }

        if (stub is PsiClassStub<*> || stub is PsiFileStub<*>) {
            for (child in stub.childrenStubs) {
                val answer = findClass(fqn, child)
                if (answer != null) return answer
            }
        }

        return null
    }/*package*/

    public fun getPsiClass(classOrObject: KtClassOrObject?): PsiClass? {
        if (classOrObject == null) return null
        return LightClassGenerationSupport.getInstance(classOrObject.project).getPsiClass(classOrObject)
    }

    public fun getLightClassAccessorMethod(accessor: KtPropertyAccessor): PsiMethod? =
            getLightClassAccessorMethods(accessor).firstOrNull()

    public fun getLightClassAccessorMethods(accessor: KtPropertyAccessor): List<PsiMethod> {
        val property = accessor.getNonStrictParentOfType<KtProperty>() ?: return emptyList()
        val wrappers = getPsiMethodWrappers(property, true)
        return wrappers.filter { wrapper -> (accessor.isGetter && !JvmAbi.isSetterName(wrapper.name)) ||
                                            (accessor.isSetter && JvmAbi.isSetterName(wrapper.name)) }
    }

    public fun getLightFieldForCompanionObject(companionObject: KtClassOrObject): PsiField? {
        val outerPsiClass = getWrappingClass(companionObject, true)
        if (outerPsiClass != null) {
            for (fieldOfParent in outerPsiClass.fields) {
                if ((fieldOfParent is KotlinLightElement<*, *>) && fieldOfParent.getOrigin() === companionObject) {
                    return fieldOfParent
                }
            }
        }
        return null
    }

    public fun getLightClassPropertyMethods(property: KtProperty): PropertyAccessorsPsiMethods {
        val getter = property.getter
        val setter = property.setter

        val getterWrapper = if (getter != null) getLightClassAccessorMethod(getter) else null
        val setterWrapper = if (setter != null) getLightClassAccessorMethod(setter) else null

        return extractPropertyAccessors(property, getterWrapper, setterWrapper)
    }

    private fun getLightClassBackingField(declaration: KtDeclaration): PsiField? {
        var psiClass: PsiClass = getWrappingClass(declaration, true) ?: return null

        if (psiClass is KotlinLightClass) {
            val origin = psiClass.getOrigin()
            if (origin is KtObjectDeclaration && origin.isCompanion()) {
                val containingClass = PsiTreeUtil.getParentOfType(origin, KtClass::class.java)
                if (containingClass != null) {
                    val containingLightClass = getPsiClass(containingClass)
                    if (containingLightClass != null) {
                        psiClass = containingLightClass
                    }
                }
            }
        }

        for (field in psiClass.fields) {
            if (field is KotlinLightField<*, *> && field.getOrigin() === declaration) {
                return field
            }
        }
        return null
    }

    public fun getLightClassPropertyMethods(parameter: KtParameter): PropertyAccessorsPsiMethods {
        return extractPropertyAccessors(parameter, null, null)
    }

    public fun getLightClassMethod(function: KtFunction): PsiMethod? {
        return getPsiMethodWrapper(function)
    }

    public fun getLightClassMethods(function: KtFunction): List<PsiMethod> {
        return getPsiMethodWrappers(function, true)
    }

    private fun getPsiMethodWrapper(declaration: KtDeclaration): PsiMethod? {
        return getPsiMethodWrappers(declaration, false).firstOrNull()
    }

    private fun getPsiMethodWrappers(declaration: KtDeclaration, collectAll: Boolean): List<PsiMethod> {
        val psiClasses = getWrappingClasses(declaration, collectAll)

        val methods = SmartList<PsiMethod>()
        for (method in psiClasses.flatMap { it.methods.asList() }) {
            try {
                if (method is KotlinLightMethod && method.getOrigin() === declaration) {
                    methods.add(method)
                    if (!collectAll) {
                        return methods
                    }
                }
            }
            catch (e: ProcessCanceledException) {
                throw e
            }
            catch (e: Throwable) {
                throw IllegalStateException(
                        "Error while wrapping declaration " + declaration.name + "Context\n:" + method, e)
            }
        }

        return methods
    }

    private fun getWrappingClasses(declaration: KtDeclaration, collectAll: Boolean): Collection<PsiClass> {
        val wrappingClass = getWrappingClass(declaration, true)
        val oldPackagePartWrappingClass = if (declaration.parent is KtFile && collectAll)
            getWrappingClass(declaration, false)
        else
            null

        return setOf(wrappingClass, oldPackagePartWrappingClass).filterNotNull()
    }

    private fun getWrappingClass(declaration: KtDeclaration, useNewPackageParts: Boolean): PsiClass? {
        var declaration = declaration
        if (declaration is KtParameter) {
            val constructorClass = KtPsiUtil.getClassIfParameterIsProperty(declaration)
            if (constructorClass != null) {
                return getPsiClass(constructorClass)
            }
        }

        if (declaration is KtPropertyAccessor) {
            val propertyParent = declaration.parent
            assert(propertyParent is KtProperty) { "JetProperty is expected to be parent of accessor" }

            declaration = propertyParent as KtProperty
        }

        if (declaration is KtConstructor<*>) {
            return getPsiClass(declaration.getContainingClassOrObject())
        }

        if (!canGenerateLightClass(declaration)) {
            // Can't get wrappers for internal declarations. Their classes are not generated during calcStub
            // with ClassBuilderMode.LIGHT_CLASSES mode, and this produces "Class not found exception" in getDelegate()
            return null
        }

        val parent = declaration.parent

        if (parent is KtFile) {
            // top-level declaration
            val fqName = if (useNewPackageParts)
                parent.javaFileFacadeFqName
            else
                PackageClassUtils.getPackageClassFqName(parent.packageFqName)

            val project = declaration.project
            return JavaElementFinder.getInstance(project).findClass(fqName.asString(), GlobalSearchScope.allScope(project))
        }
        else if (parent is KtClassBody) {
            assert(parent.parent is KtClassOrObject)
            return getPsiClass(parent.parent as KtClassOrObject)
        }

        return null
    }

    public fun canGenerateLightClass(declaration: KtDeclaration): Boolean {
        //noinspection unchecked
        return PsiTreeUtil.getParentOfType(declaration, KtFunction::class.java, KtProperty::class.java) == null
    }

    private fun extractPropertyAccessors(
            ktDeclaration: KtDeclaration,
            specialGetter: PsiMethod?, specialSetter: PsiMethod?): PropertyAccessorsPsiMethods {
        var getterWrapper = specialGetter
        var setterWrapper = specialSetter
        val additionalAccessors = arrayListOf<PsiMethod>()

        val wrappers = getPsiMethodWrappers(ktDeclaration, true).filter {
            JvmAbi.isGetterName(it.name) || JvmAbi.isSetterName(it.name)
        }

        for (wrapper in wrappers) {
            if (JvmAbi.isSetterName(wrapper.getName())) {
                if (setterWrapper == null || setterWrapper === specialSetter) {
                    setterWrapper = wrapper
                }
                else {
                    additionalAccessors.add(wrapper)
                }
            }
            else {
                if (getterWrapper == null || getterWrapper == specialGetter) {
                    getterWrapper = wrapper
                }
                else {
                    additionalAccessors.add(wrapper)
                }
            }
        }

        val backingField = getLightClassBackingField(ktDeclaration)
        return PropertyAccessorsPsiMethods(getterWrapper, setterWrapper, backingField, additionalAccessors)
    }

    public fun buildLightTypeParameterList(
            owner: PsiTypeParameterListOwner,
            declaration: KtDeclaration): PsiTypeParameterList {
        val builder = KotlinLightTypeParameterListBuilder(owner.manager)
        if (declaration is KtTypeParameterListOwner) {
            val parameters = declaration.typeParameters
            for (i in parameters.indices) {
                val jetTypeParameter = parameters.get(i)
                val name = jetTypeParameter.name
                val safeName = name ?: "__no_name__"
                builder.addParameter(KotlinLightTypeParameter(owner, i, safeName))
            }
        }
        return builder
    }

    public class PropertyAccessorsPsiMethods(public val getter: PsiMethod?,
                                             public val setter: PsiMethod?,
                                             public val backingField: PsiField?,
                                             additionalAccessors: List<PsiMethod>) : Iterable<PsiMethod> {
        private val allMethods = arrayListOf<PsiMethod>()
        val allDeclarations = arrayListOf<PsiNamedElement>()

        init {
            listOf(getter, setter).filterNotNullTo(allMethods)
            listOf<PsiNamedElement?>(getter, setter, backingField).filterNotNullTo(allDeclarations)
            allDeclarations.addAll(additionalAccessors)
            additionalAccessors.filterIsInstanceTo<PsiMethod, MutableList<PsiMethod>>(allMethods)
        }

        override fun iterator(): Iterator<PsiMethod> = allMethods.iterator()
    }
}
