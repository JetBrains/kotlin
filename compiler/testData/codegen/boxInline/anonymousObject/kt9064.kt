// FILE: 1.kt

package test

class Test(val _member: String) {
    val _parameter: Z =  test {
        object : Z {
            override val property = _member
        }
    }
}

interface Z {
    val property: String
}

inline fun test(s: () -> Z): Z {
    return s()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {

    val test = Test("OK")

    return test._parameter.property
}
