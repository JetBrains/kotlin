enum class A {
    OK
}

enum class B {
    FAIL
}

fun f() = A.OK

fun box(): String {
    return when (f()) {
        B.FAIL -> "fail"
        A.OK -> "OK"
    }
}

// 0 TABLESWITCH
// 0 LOOKUPSWITCH
