// This test depends on line numbers
// WITH_RUNTIME
// FILE: 1.kt

package test

interface SuspendRunnable {
    suspend fun run()
}

inline suspend fun inlineMe1(crossinline c: suspend () -> Unit): SuspendRunnable =
    object : SuspendRunnable {
        override suspend fun run() {
            c()
        }
    }

inline suspend fun inlineMe2(crossinline c: suspend () -> Unit): suspend () -> Unit =
    {
        c()
    }

// FILE: 2.kt

import kotlin.coroutines.*
import kotlin.coroutines.intrinsics.*
import test.*

fun builder(c: suspend () -> Unit) {
    c.startCoroutine(object: Continuation<Unit> {
        override val context = EmptyCoroutineContext
        override fun resumeWith(result: Result<Unit>) {
            result.getOrThrow()
        }
    })
}

fun box(): String {
    var res = ""
    builder {
        inlineMe1 {
            res += "O"
        }.run()
        inlineMe2 {
            res += "K"
        }()
    }
    return res
}

// FILE: 1.smap
SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineMe2$2
*L
1#1,23:1
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineMe1$2
*L
1#1,23:1
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineMe1$2$run$1
*L
1#1,23:1
*E

// FILE: 2.smap
SMAP
2.kt
Kotlin
*S Kotlin
*F
+ 1 2.kt
_2Kt$box$1
+ 2 1.kt
test/_1Kt
*L
1#1,29:1
12#2,5:30
19#2,3:35
*E
*S KotlinDebug
*F
+ 1 2.kt
_2Kt$box$1
*L
19#1,5:30
22#1,3:35
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineMe2$2
+ 2 2.kt
_2Kt$box$1
*L
1#1,23:1
23#2,2:24
*E

SMAP
1.kt
Kotlin
*S Kotlin
*F
+ 1 1.kt
test/_1Kt$inlineMe1$2
+ 2 2.kt
_2Kt$box$1
*L
1#1,23:1
20#2,2:24
*E