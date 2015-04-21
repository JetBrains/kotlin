// !CHECK_TYPE

fun foo() {
    val a = object {
        val b = object {
            val c = 42
        }
    }

    checkSubtype<Int>(a.b.c)
}