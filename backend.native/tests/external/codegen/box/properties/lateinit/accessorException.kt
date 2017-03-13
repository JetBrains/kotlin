// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

public class A {

    fun getMyStr(): String {
        try {
            val a = str
        } catch (e: RuntimeException) {
            return "OK"
        }

        return "FAIL"
    }

    private companion object {
        private lateinit var str: String
    }
}

fun box(): String {
    val a = A()
    return a.getMyStr()
}