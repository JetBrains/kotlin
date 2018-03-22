fun test() {
    JavaClass.getNull().toBoolean() // ISE in runtime
    JavaClass.getNull().toInt()     // OK
    "123".toBoolean()               // OK
    null.toBoolean()                // Compilation error, no inspection message

    JavaClass.getNull().contentNotInline() // OK
    JavaClass.getMy().contentNonExtensionInlineFun() // OK
}

fun String.contentNotInline() {}

class My {
    inline fun contentNonExtensionInlineFun() {}
}