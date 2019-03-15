enum class A {
    O, K
}

enum class B {
    O, K
}

fun box(): String {
    val a = A.O
    val r1 = when (a) {
        A.O -> "O"
        A.K -> "K"
        B.O -> "fail 1"
        B.K -> "fail 2"
    }

    val b = B.K
    val r2 = when (b) {
        A.O -> "fail 3"
        A.K -> "fail 4"
        B.O -> "O"
        B.K -> "K"
    }

    return r1 + r2
}

// 0 TABLESWITCH
// 0 LOOKUPSWITCH
