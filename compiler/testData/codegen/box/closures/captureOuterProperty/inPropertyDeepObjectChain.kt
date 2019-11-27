// IGNORE_BACKEND_FIR: JVM_IR
interface T {
    fun result(): String
}

class A(val x: String) {
    fun foo() = object : T {
        fun bar() = object : T {
            fun baz() = object : T {
                val y = x
                override fun result() = y
            }
            override fun result() = baz().result()
        }
        override fun result() = bar().result()
    }
}

fun box() = A("OK").foo().result()
