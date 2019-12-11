// !CHECK_TYPE

object Obj {
    fun foo() {}
    val bar = 2
}

fun test() {
    checkSubtype<() -> Unit>(Obj::foo)
    checkSubtype<() -> Int>(Obj::bar)
}
