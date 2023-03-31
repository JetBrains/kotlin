// ISSUE: KT-56520
// FULL_JDK
// FILE: some/HashMap.java
package some;

public class HashMap<K, V> extends java.util.HashMap<K, V> {}

// FILE: test.kt
import java.util.*
import some.*

fun foo(): Any? = null

fun test() {
    // Should be OVERLOAD_RESOLUTION_AMBIGUITY in K2
    val map = foo() as <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<String, String><!>
    // Should be OVERLOAD_RESOLUTION_AMBIGUITY in K2
    val map2 = <!OVERLOAD_RESOLUTION_AMBIGUITY!>HashMap<!><String, String>()
}
