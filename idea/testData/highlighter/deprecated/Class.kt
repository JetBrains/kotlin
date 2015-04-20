package test

import java.util.ArrayList

deprecated("Use A instead") open class MyClass {}

fun test() {
    val a : <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyClass' is deprecated. Use A instead">MyClass</warning>? = null
    val b = <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyClass' is deprecated. Use A instead">MyClass</warning>()
    val c = ArrayList<<warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyClass' is deprecated. Use A instead">MyClass</warning>>()

    a == b && a == c
}

class Test(): <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyClass' is deprecated. Use A instead">MyClass</warning>() {}

class Test2(<warning descr="[UNUSED_PARAMETER] Parameter 'param' is never used">param</warning>: <warning descr="[DEPRECATED_SYMBOL_WITH_MESSAGE] 'MyClass' is deprecated. Use A instead">MyClass</warning>) {}

// NO_CHECK_INFOS
// NO_CHECK_WEAK_WARNINGS
