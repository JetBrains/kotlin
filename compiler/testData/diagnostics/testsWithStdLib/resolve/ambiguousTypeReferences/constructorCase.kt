// ISSUE: KT-56520 (case 2)
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
    // K1 resolves this to kotlin.collections.HashMap.
    // K2 resolves this to java.util.HashMap
    <!DEBUG_INFO_CALL("fqName: kotlin.collections.HashMap.<init>; typeCall: function")!>HashMap<String, String>()<!>
