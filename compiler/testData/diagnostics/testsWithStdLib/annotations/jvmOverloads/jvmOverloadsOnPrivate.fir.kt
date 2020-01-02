// !DIAGNOSTICS: -UNUSED_PARAMETER
class C {
    @kotlin.jvm.JvmOverloads private fun foo(s: String = "OK") {
    }

    @kotlin.jvm.JvmOverloads fun bar(s: String = "OK") {
    }
}

fun foo() {
    @kotlin.jvm.JvmOverloads fun quux(s: String = "OK") {
    }

    class D {
        @kotlin.jvm.JvmOverloads fun foo(s: String = "OK") {
        }
    }

    val x = object {
        @kotlin.jvm.JvmOverloads fun foo(s: String = "OK") {
        }
    }
}