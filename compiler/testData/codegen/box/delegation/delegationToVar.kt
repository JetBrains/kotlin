// ISSUE: KT-65920

interface I {
    fun foo()
}

class Test2(var j: I) : I by j

fun box(): String {
    var result = ""

    val x = Test2(object : I { override fun foo() { result += "1" }})
    x.foo()
    x.j = object : I { override fun foo() { result += "2" }}
    x.foo()
    return when (result) {
        "11" -> "OK"
        else -> "Fail: $result"
    }
}
