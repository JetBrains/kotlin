// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtUserType

enum class MyClass(val i: Int = foo()) : <expr>UnresolvedInterface</expr> {
    ENTRY1,
    ENTRY2(42);

    fun boo(a: String) {

    }
}

fun foo() = 42