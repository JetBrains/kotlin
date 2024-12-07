// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-56520 (case 4, unbound)
// FIR_DUMP
// FULL_JDK

// FILE: some/HashMap.java

package some;

public class HashMap<K, V> extends java.util.HashMap<K, V> {}

// FILE: test.kt

import java.util.*
import some.*

fun test() =
    // Ok in both K1 & K2.
    // K1 & K2 resolve this to kotlin.collections.HashMap.
    HashMap<String, String>::size
