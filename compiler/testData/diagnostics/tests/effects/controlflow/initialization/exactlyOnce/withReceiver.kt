// !DIAGNOSTICS: -INVISIBLE_MEMBER -INVISIBLE_REFERENCE

import kotlin.internal.*

fun <T, R> T.myLet(@CalledInPlace(InvocationCount.EXACTLY_ONCE) block: (T) -> R) = block(this)

fun initializationWithReceiver(y: String) {
    val x: Int
    y.myLet { x = 42 }
    x.inc()
}

fun initializationWithElvis(y: String?) {
    val x: Int
    y?.myLet { x = 42 }
    <!UNINITIALIZED_VARIABLE!>x<!>.inc()
}

fun sanityCheck(x: Int, y: String): Int {
    y.let { return x }
}
