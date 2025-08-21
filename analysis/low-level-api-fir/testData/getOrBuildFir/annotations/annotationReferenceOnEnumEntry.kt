// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtTypeReference

enum class MyClass(val i: Int = foo()) {
    @<expr>Anno</expr>("str") ENTRY1,
    ENTRY2(42);

    fun boo(a: String) {

    }
}

fun foo() = 42

annotation class Anno(val value: String)