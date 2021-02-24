// !DUMP_CFG
inline fun run(block: () -> Unit) {
    block()
}

val x1 = 1

var x2: Int = 1
    get() = 1
    set(value) {
        field = 1
    }

val x3 = <!PROPERTY_INITIALIZER_NO_BACKING_FIELD!>run {
    fun foo() {
        val c = 1 + 1
        throw Exception()
    }

    class InitializerLocalClass {
        init {
            throw Exception()
            1
        }
    }

    throw Exception()
}<!>
    get() {
        class GetterLocalClass {
            init {
                throw Exception()
            }
        }
    }

val x4 = try {
    1
} catch (e: Exception) {
    2
} finally {
    0
}
