/*
 * Copyright 2000-2017 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.asJava.builder

import com.intellij.psi.PsiClass
import com.intellij.psi.PsiClassType
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.java.stubs.PsiJavaFileStub
import com.intellij.psi.stubs.StubElement
import org.jetbrains.kotlin.asJava.LightClassUtil
import org.jetbrains.kotlin.asJava.LightClassUtil.findClass
import org.jetbrains.kotlin.asJava.classes.KtLightClass
import org.jetbrains.kotlin.asJava.classes.getOutermostClassOrObject
import org.jetbrains.kotlin.asJava.elements.KtLightField
import org.jetbrains.kotlin.asJava.elements.KtLightFieldImpl
import org.jetbrains.kotlin.asJava.elements.KtLightMethod
import org.jetbrains.kotlin.asJava.elements.KtLightMethodImpl
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.debugText.getDebugText
import org.jetbrains.kotlin.resolve.diagnostics.Diagnostics
import org.jetbrains.kotlin.utils.KotlinExceptionWithAttachments

interface LightClassDataHolder {
    val javaFileStub: PsiJavaFileStub
    val extraDiagnostics: Diagnostics

    fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass): LightClassData

    interface ForClass : LightClassDataHolder {
        fun findDataForDefaultImpls(classOrObject: KtClassOrObject) = findData {
            it.findDelegate(classOrObject).findInnerClassByName(JvmAbi.DEFAULT_IMPLS_CLASS_NAME, false)
            ?: throw IllegalStateException("Couldn't get delegate for $this\n in ${DebugUtil.stubTreeToString(it)}")
        }

        fun findDataForClassOrObject(classOrObject: KtClassOrObject): LightClassData = findData { it.findDelegate(classOrObject) }
    }

    interface ForFacade : LightClassDataHolder {
        fun findDataForFacade(classFqName: FqName): LightClassData = findData { it.findDelegate(classFqName) }
    }

    interface ForScript : ForClass {
        fun findDataForScript(scriptFqName: FqName): LightClassData = findData { it.findDelegate(scriptFqName) }
    }
}

interface LightClassData {
    val clsDelegate: PsiClass

    val supertypes: Array<PsiClassType> get() { return clsDelegate.superTypes }

    fun getOwnFields(containingClass: KtLightClass): List<KtLightField>
    fun getOwnMethods(containingClass: KtLightClass): List<KtLightMethod>
}

class LightClassDataImpl(override val clsDelegate: PsiClass) : LightClassData {
    override fun getOwnFields(containingClass: KtLightClass) = KtLightFieldImpl.fromClsFields(clsDelegate, containingClass)

    override fun getOwnMethods(containingClass: KtLightClass) = KtLightMethodImpl.fromClsMethods(clsDelegate, containingClass)
}

object InvalidLightClassDataHolder : LightClassDataHolder.ForClass {
    override val javaFileStub: PsiJavaFileStub
        get() = shouldNotBeCalled()

    override val extraDiagnostics: Diagnostics
        get() = shouldNotBeCalled()

    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass) = shouldNotBeCalled()

    private fun shouldNotBeCalled(): Nothing = throw UnsupportedOperationException("Should not be called")
}

class LightClassDataHolderImpl(
        override val javaFileStub: PsiJavaFileStub,
        override val extraDiagnostics: Diagnostics
) : LightClassDataHolder.ForClass, LightClassDataHolder.ForFacade, LightClassDataHolder.ForScript {
    override fun findData(findDelegate: (PsiJavaFileStub) -> PsiClass) = findDelegate(javaFileStub).let(::LightClassDataImpl)
}

fun PsiJavaFileStub.findDelegate(classOrObject: KtClassOrObject): PsiClass {
    findClass(this) {
        ClsWrapperStubPsiFactory.getOriginalElement(it as StubElement<*>) == classOrObject
    }?.let { return it }

    val outermostClassOrObject = getOutermostClassOrObject(classOrObject)
    val ktFileText: String? = try {
        outermostClassOrObject.containingFile.text
    }
    catch (e: Exception) {
        "Can't get text for outermost class"
    }

    val stubFileText = DebugUtil.stubTreeToString(this)
    throw KotlinExceptionWithAttachments("Couldn't get delegate for class")
            .withAttachment(classOrObject.name ?: "unnamed class or object", classOrObject.getDebugText())
            .withAttachment("file.kt", ktFileText)
            .withAttachment("stub text", stubFileText)
}

fun PsiJavaFileStub.findDelegate(classFqName: FqName): PsiClass {
    return findClass(this) {
        classFqName.asString() == it.qualifiedName
    } ?: throw IllegalStateException("Facade class $classFqName not found; classes in Java file stub: ${collectClassNames(this)}")
}


private fun collectClassNames(javaFileStub: PsiJavaFileStub): String {
    val names = mutableListOf<String>()
    LightClassUtil.findClass(javaFileStub) { cls ->
        names.add(cls.qualifiedName ?: "<null>")
        false
    }
    return names.joinToString(prefix = "[", postfix = "]")
}