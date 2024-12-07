<expr>
enum class MyClass(val i: Int = foo()) : UnresolvedInterface {
    ENTRY1,
    ENTRY2(42);

    fun boo(a: String) {

    }
}
</expr>

fun foo() = 42