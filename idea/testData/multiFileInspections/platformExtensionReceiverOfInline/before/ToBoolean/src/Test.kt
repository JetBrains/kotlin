fun test() {
    JavaClass.getNull().toBoolean() // ISE in runtime
    JavaClass.getNull().toInt()     // OK
    "123".toBoolean()               // OK
    null.toBoolean()                // Compilation error, no inspection message

    JavaClass.getNull().contentNotInline() // OK
    JavaClass.getMy().contentNonExtensionInlineFun() // OK

    val res = JavaClass.getNull()
    if (res != null) {
        res.toBoolean() // OK: KT-29499
    }
}

inline fun String.toBoolean(): Boolean = true

fun String.contentNotInline() {}

class My {
    inline fun contentNonExtensionInlineFun() {}
}