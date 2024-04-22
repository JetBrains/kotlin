// IGNORE_BACKEND_K1: ANY
// ISSUE: KT-65299

val foo = object {
    inner class Inner {
        val field: Any

        constructor(field: Any) {
            this.field = field
        }

        val property get() = field
    }

    override fun toString() = Inner("OK").property as String
}

fun box(): String = foo.toString()
