// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: base
// FILE: base.kt

interface PsiClass {
    fun foo(): String?
}

interface UClass : PsiClass {
    override fun foo(): String?
}

abstract class BaseKotlinUClass(
    psi: PsiClass,
    val w: String,
) : UClass, PsiClass by psi

// MODULE: main(base)
// FILE: main.kt

class A(psi: PsiClass) : BaseKotlinUClass(psi, "K")

fun bar(uClass: UClass): String = uClass.foo()!! + (uClass as BaseKotlinUClass).w

fun box(): String = bar(A(object : PsiClass {
    override fun foo(): String? = "O"
}))
