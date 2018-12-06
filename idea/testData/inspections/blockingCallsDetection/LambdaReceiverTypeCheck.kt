@file:Suppress("UNUSED_PARAMETER")

import java.lang.Thread.sleep
import kotlin.coroutines.RestrictsSuspension

suspend fun testFunction() {
    // @RestrictsSuspension annotation allows blocking calls
    withRestrictedReceiver({Thread.sleep(0)}, {Thread.sleep(1)})

    withSimpleReceiver({Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(2)})
}

fun withRestrictedReceiver(firstParam: suspend Test1.() -> Unit, secondParam: () -> Unit) {}

fun withSimpleReceiver(firstParam: suspend Test2.() -> Unit) {}

@RestrictsSuspension
class Test1

class Test2