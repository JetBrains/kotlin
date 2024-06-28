// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtAnnotationEntry

<expr>@Anno</expr>
enum class MyClass(val i: Int = foo()) {
    ENTRY1,
    ENTRY2(42);

    fun boo(a: String) {

    }
}

fun foo() = 42