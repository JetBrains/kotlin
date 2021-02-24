class Module

fun getModule(): Module? = null

fun getInt(): Int? = null

fun test_1(modules: Collection<Module>, b: Boolean) {
    val res = modules.groupBy { module ->
            if (b) module else module
        }
}

fun test_2() {
    val x = run {
        try {
            ""
        } finally {
            getInt()
        }
    }
}