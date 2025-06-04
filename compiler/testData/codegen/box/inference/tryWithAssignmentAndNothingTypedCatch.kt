// IGNORE_BACKEND_K2: ANY
// ISSUE: KT-80208

class A(val s: String)

var result: A? = null

fun test(json: String) {
    result = try {
        build(json) {
            A(json)
        }
    } catch (e: Exception) {
        return
    }
}

fun <T> build(str: String, builder: (String) -> T): T {
    return builder.invoke(str)
}

fun box(): String {
    test("OK")

    return result!!.s
}