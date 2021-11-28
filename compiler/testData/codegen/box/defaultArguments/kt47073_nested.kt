// IGNORE_BACKEND: JS

class Test<T: Char>(val k: T) {
    fun test(): String {
        fun nested(x: T = k): String {
            return "O$x"
        }
        return nested()
    }
}

fun box() = Test('K').test()
