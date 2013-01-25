class A {
    private val sb: StringBuilder = StringBuilder()

    fun String.plus() {
        sb.append(this)
    }

    fun foo(): String {
        +"OK"
        return sb.toString()!!
    }
}

fun box(): String = A().foo()
