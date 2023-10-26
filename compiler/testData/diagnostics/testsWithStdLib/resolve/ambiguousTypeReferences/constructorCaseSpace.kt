// FIR_IDENTICAL

// ISSUE: KT-56520 (case 2, Space subcase)
// FIR_DUMP
// FULL_JDK

// FILE: 1.kt

package kotlinx.coroutines.sync;

interface Semaphore {}

fun Semaphore(arg0: Int, arg: Int = 0) = 1

// FILE: 2.java
package java.util.concurrent;

public class Semaphore {
    public Semaphore(arg: Int) {}
}

// FILE: test.kt
import java.util.concurrent.*
import kotlinx.coroutines.sync.*

fun test() =
    // K1/K2: resolve to kotlinx.coroutines.sync.Semaphore
    // K2 ignores java.util.concurrent because of interface/class classifier ambiguity
    <!DEBUG_INFO_CALL("fqName: kotlinx.coroutines.sync.Semaphore; typeCall: function")!>Semaphore(1)<!>
