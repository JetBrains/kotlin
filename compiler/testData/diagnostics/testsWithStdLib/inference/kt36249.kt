// DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE -UNUSED_EXPRESSION

import kotlin.reflect.KClass
fun <T : PsiElement> select(vararg classes: KClass<out T>): T? {
    return null
}
interface PomRenameableTarget
interface PsiElement
interface PsiMethod : PsiElement, PomRenameableTarget
interface PsiClass : PsiElement, PomRenameableTarget

class A {
    val inv get() = select(PsiMethod::class, PsiClass::class)
}

fun main() {
    <!DEBUG_INFO_EXPRESSION_TYPE("PsiElement?")!>A().inv<!>
}
