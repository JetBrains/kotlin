// !CHECK_TYPE

class Foo

fun Foo?.bar() {}

fun test() {
    val r1 = Foo ?:: bar
    checkSubtype<(Foo?) -> Unit>(r1)

    val r2 = Foo ? :: bar
    checkSubtype<(Foo?) -> Unit>(r2)

    val r3 = Foo ? ? :: bar
    checkSubtype<(Foo?) -> Unit>(r3)
}
