// IGNORE_FIR

package test

import java.util.ArrayList

@Deprecated("Use A instead") open class MyClass {
    val test = this
}

fun test() {
    val a : <warning descr="[DEPRECATION] 'MyClass' is deprecated. Use A instead">MyClass</warning>? = null
    val b = <warning descr="[DEPRECATION] 'MyClass' is deprecated. Use A instead">MyClass</warning>()
    val c = ArrayList<<warning descr="[DEPRECATION] 'MyClass' is deprecated. Use A instead">MyClass</warning>>()

    a == b && a == c
}

class Test(): <warning descr="[DEPRECATION] 'MyClass' is deprecated. Use A instead">MyClass</warning>() {}

class Test2(<warning descr="[UNUSED_PARAMETER] Parameter 'param' is never used">param</warning>: <warning descr="[DEPRECATION] 'MyClass' is deprecated. Use A instead">MyClass</warning>) {}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
