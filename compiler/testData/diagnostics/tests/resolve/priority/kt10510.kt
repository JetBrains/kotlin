// !CHECK_TYPE
// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE

class ClassA {
    fun method1() = this

    fun String.method2() {
        method1() checkType { _<String>() }
        this.method1() checkType { _<String>() }
    }
}

fun String.method1() = this