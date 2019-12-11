open class A {
    class B : A() {
        val a = "FAIL"
    }

    class C : A() {
        val a = "FATAL"
    }

    fun foo(): String {
        if (this is B) return a
        else if (this is C) return a
        return "OK"
    }
}

fun A?.bar() {
    if (this != null) foo()
}

fun A.gav() = if (this is A.B) a else ""

class C {
    fun A?.complex(): String {
        if (this is A.B) return a
        else if (this != null) return foo()
        else return ""
    }
}
