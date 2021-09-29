var result = ""

abstract class Base {
    fun base1(vararg s: String) {
        if (s.size != 1) throw AssertionError("Fail size: ${s.size}")
        result += s[0]
    }

    fun base2(s: String) {
        result += s
    }
}

fun id(f: (String) -> Unit): (String) -> Unit = f

class Derived : Base() {
    init {
        id { base1(it) }.invoke("1")
        id(::base1).invoke("2")
        id { base2(it) }.invoke("3")
        id(::base2).invoke("4")

        id { derived1(it) }.invoke("5")
        id(::derived1).invoke("6")
        id { derived2(it) }.invoke("7")
        id(::derived2).invoke("8")
    }

    private fun derived1(vararg s: String) {
        if (s.size != 1) throw AssertionError("Fail size: ${s.size}")
        result += s[0]
    }

    private fun derived2(s: String) {
        result += s
    }
}

fun box(): String {
    Derived()
    return if (result == "12345678") "OK" else "Fail result: $result"
}
