// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-68796
class Foo {
    val alias: Foo = this
    fun identityFunction(): Foo = this
    val identityProperty: () -> Foo = TODO()
    val nestedIdentityProperty: () -> () -> Foo = TODO()

    operator fun get(index: Int): Foo = this
    operator fun invoke(arg: String) {}
}

fun huh(arg: Foo?) {
    arg?.alias
    arg?.alias[42]
    arg?.alias("")
    arg?.alias[42]("")

    arg?.identityFunction()
    arg?.identityFunction()[42]
    arg?.identityFunction()("")
    arg?.identityFunction()[42]("")

    arg?.identityProperty()
    arg?.identityProperty()[42]
    arg?.identityProperty()("")
    arg?.identityProperty()[42]("")

    arg?.nestedIdentityProperty()()
    arg?.nestedIdentityProperty()()[42]
    arg?.nestedIdentityProperty()()("")
    arg?.nestedIdentityProperty()()[42]("")
}
