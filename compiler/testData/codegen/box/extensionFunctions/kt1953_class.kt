// IGNORE_BACKEND: JS_IR
class A {
    private val sb: StringBuilder = StringBuilder()

    operator fun String.unaryPlus() {
        sb.append(this)
    }

    fun foo(): String {
        +"OK"
        return sb.toString()!!
    }
}

fun box(): String = A().foo()
