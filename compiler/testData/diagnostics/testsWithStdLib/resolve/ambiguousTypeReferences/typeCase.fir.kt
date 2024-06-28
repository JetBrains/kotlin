// ISSUE: KT-56520 (cases 1, 5, 6)
// FIR_DUMP
// FULL_JDK

// FILE: some/HashMap.java

package some;

public class HashMap<K, V> extends java.util.HashMap<K, V> {}

// FILE: test.kt

import java.util.*
import some.*

fun foo(): Any? = null

// K1: misses both some.HashMap and java.util.HashMap due to ambiguous classifiers, takes kotlin.collections.HashMap from the next scope
// K2: properly reports ambiguity
@Suppress("UNCHECKED_CAST")
fun test() = foo() as <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<String, String><!> // Case 1

// Similar behavior to test() above
val bar: <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<String, String><!>? = null      // Case 5

// Similar behavior to test() above
val baz = foo() is <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<*, *><!>              // Case 6

