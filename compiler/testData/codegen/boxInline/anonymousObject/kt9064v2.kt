// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

class Test(val _member: String) {
    val _parameter: Z<Z<String>> =  test {
        object : Z<Z<String>> {
            override val property = test {
                object : Z<String> {
                    override val property = _member
                }
            }
        }
    }
}

interface Z<T> {
    val property: T
}

inline fun <T> test(s: () -> Z<T>): Z<T> {
    return s()
}

// FILE: 2.kt

//NO_CHECK_LAMBDA_INLINING
import test.*

fun box(): String {

    val test = Test("OK")

    return test._parameter.property.property
}
