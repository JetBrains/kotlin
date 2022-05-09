// ISSUE: KT-51228

interface A {
    fun foo()
}

fun Any.test_1() {
    when(this) {
        is A -> {
            foo() // ok
            ::foo // UNRESOLVED_REFERENCE, should be ok
        }
        else -> throw Exception()
    }
}

fun Any.test_2() {
    when(this) {
        is A -> {
            foo(); // ok
            { foo() }
        }
        else -> {}
    }
}

fun Any.test_3() {
    if (this is A) {
        val f = ::foo // ok
    }
}
