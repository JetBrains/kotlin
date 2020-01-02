// !DIAGNOSTICS: -UNUSED_EXPRESSION

class Unrelated()

class Test(val name: String = "") {
    init {
        Unrelated::name
        Unrelated::foo
    }

    fun foo() {}
}
