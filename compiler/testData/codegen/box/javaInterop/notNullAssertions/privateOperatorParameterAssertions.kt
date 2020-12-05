// TARGET_BACKEND: JVM
// WITH_REFLECT

var result: String? = "fail"

class X

class A {
    // There should be a null check on the extension receiver of private
    // operator functions, but no null checks on all other arguments.

    private operator fun X.get(name: String) = "$name$result"

    private operator fun X.set(name: String, v: String) {
        result = v
    }

    private fun getMethod(name: String) =
        A::class.java.getDeclaredMethods().first { it.name == name }

    fun test(): String {
        val setter = getMethod("set")
        try {
            setter.invoke(this, X(), null, null)
        } catch (e: Throwable) {
            return "Fail 1"
        }

        try {
            setter.invoke(this, null, null, null)
            return "Fail 2"
        } catch (e: Throwable) {
            // expected
            if (e.cause !is NullPointerException) {
                return "Fail 3"
            }
        }

        val getter = getMethod("get")
        val s = try {
            getter.invoke(this, X(), null) as String
        } catch (e: Throwable) {
            return "Fail 4"
        }

        try {
            getter.invoke(this, null, null)
            return "Fail 5"
        } catch (e: Throwable) {
            // expected
            if (e.cause !is NullPointerException) {
                return "Fail 6"
            }
        }

        return if (s == "nullnull") "OK" else "Fail 7"
    }
}

fun box(): String {
    return A().test()
}
