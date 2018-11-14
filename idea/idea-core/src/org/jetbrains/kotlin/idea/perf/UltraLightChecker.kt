/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.perf

import com.intellij.openapi.Disposable
import com.intellij.openapi.util.Disposer
import com.intellij.psi.*
import com.intellij.testFramework.UsefulTestCase
import org.jetbrains.annotations.TestOnly
import org.jetbrains.kotlin.asJava.LightClassGenerationSupport
import org.jetbrains.kotlin.asJava.classes.KtLightClassForSourceDeclaration
import org.jetbrains.kotlin.asJava.classes.KtUltraLightClass
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtFile
import org.junit.Assert

fun UsefulTestCase.forceUsingUltraLightClassesForTest() {
    KtUltraLightClass.forceUsingUltraLightClasses = true
    Disposer.register(testRootDisposable, Disposable {
        KtUltraLightClass.forceUsingUltraLightClasses = false
    })
}

@TestOnly
object UltraLightChecker {
    fun checkClassEquivalence(file: KtFile) {
        for (ktClass in allClasses(file)) {
            checkClassEquivalence(ktClass)
        }
    }

    fun allClasses(file: KtFile): List<KtClassOrObject> =
        SyntaxTraverser.psiTraverser(file).filter(KtClassOrObject::class.java).toList()

    fun checkClassEquivalence(ktClass: KtClassOrObject): KtUltraLightClass? {
        val gold = KtLightClassForSourceDeclaration.create(ktClass)
        val ultraLightClass = LightClassGenerationSupport.getInstance(ktClass.project).createUltraLightClass(ktClass) ?: return null

        if (gold != null) {
            Assert.assertFalse(gold.javaClass.name.contains("Ultra"))
        }

        val goldText = gold?.renderClass().orEmpty()
        val ultraText = ultraLightClass.renderClass()

        if (goldText != ultraText) {
            Assert.assertEquals(
                "// Classic implementation:\n$goldText",
                "// Ultra-light implementation:\n$ultraText"
            )
        }
        return ultraLightClass
    }

    private fun PsiAnnotation.renderAnnotation() =
        "@" + qualifiedName + "(" + parameterList.attributes.joinToString { it.name + "=" + (it.value?.text ?: "?") } + ")"

    private fun PsiModifierListOwner.renderModifiers(): String {
        val buffer = StringBuilder()
        for (annotation in annotations) {
            buffer.append(annotation.renderAnnotation())
            buffer.append(if (this is PsiParameter) " " else "\n")
        }
        for (modifier in PsiModifier.MODIFIERS.filter(::hasModifierProperty)) {
            buffer.append(modifier).append(" ")
        }
        return buffer.toString()
    }

    private fun PsiType.renderType() = getCanonicalText(true)

    private fun PsiReferenceList?.renderRefList(keyword: String): String {
        if (this == null || this.referencedTypes.isEmpty()) return ""
        return " " + keyword + " " + referencedTypes.joinToString { it.renderType() }
    }

    private fun PsiVariable.renderVar(): String {
        var result = this.renderModifiers() + type.renderType() + " " + name
        if (this is PsiParameter && this.isVarArgs) {
            result += " /* vararg */"
        }
        computeConstantValue()?.let { result += " /* constant value $it */" }
        return result
    }

    private fun PsiTypeParameterListOwner.renderTypeParams() =
        if (typeParameters.isEmpty()) ""
        else "<" + typeParameters.joinToString {
            val bounds =
                if (it.extendsListTypes.isNotEmpty())
                    " extends " + it.extendsListTypes.joinToString(" & ", transform = { it.renderType() })
                else ""
            it.name!! + bounds
        } + "> "

    private fun PsiMethod.renderMethod() =
        renderModifiers() +
                (if (isVarArgs) "/* vararg */ " else "") +
                renderTypeParams() +
                (returnType?.renderType() ?: "") + " " +
                name +
                "(" + parameterList.parameters.joinToString { it.renderModifiers() + it.type.renderType() } + ")" +
                (this as? PsiAnnotationMethod)?.defaultValue?.let { " default " + it.text }.orEmpty() +
                throwsList.referencedTypes.let { thrownTypes ->
                    if (thrownTypes.isEmpty()) ""
                    else " throws " + thrownTypes.joinToString { it.renderType() }
                } +
                ";"

    private fun PsiClass.renderClass(): String {
        val classWord = when {
            isAnnotationType -> "@interface"
            isInterface -> "interface"
            isEnum -> "enum"
            else -> "class"
        }

        return renderModifiers() +
                classWord + " " +
                name + " /* " + qualifiedName + "*/" +
                renderTypeParams() +
                extendsList.renderRefList("extends") +
                implementsList.renderRefList("implements") +
                " {\n" +
                (if (isEnum) fields.filterIsInstance<PsiEnumConstant>().joinToString(",\n") { it.name } + ";\n\n" else "") +
                fields.filterNot { it is PsiEnumConstant }.map { it.renderVar().prependIndent("  ") + ";\n\n" }.sorted().joinToString("") +
                methods.map { it.renderMethod().prependIndent("  ") + "\n\n" }.sorted().joinToString("") +
                innerClasses.map { "class ${it.name} ...\n\n".prependIndent("  ") }.sorted().joinToString("") +
                "}"
    }

}
