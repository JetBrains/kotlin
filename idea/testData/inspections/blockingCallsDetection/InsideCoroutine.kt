@file:Suppress("UNUSED_PARAMETER")

import kotlin.coroutines.*
import java.lang.Thread.sleep

class InsideCoroutine {
    suspend fun example1() {
        Thread.<warning descr="Inappropriate blocking method call">sleep</warning>(1);
    }
}