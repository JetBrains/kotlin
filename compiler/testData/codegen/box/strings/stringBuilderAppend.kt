// IGNORE_BACKEND: JS

class A() {

    override fun toString(): String {
        return "A"
    }
}

fun box() : String {

    val s = "1" + "2" + 3 + 4L + 5.0 + 6F + '7' + A()

    if (s != "12345.06.07A") return "fail $s"

    return "OK"
}


// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: IGNORED_IN_JS
