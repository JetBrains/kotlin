// FIR_IDENTICAL
// ISSUE: KT-56520 (case 3)
// FIR_DUMP
// FULL_JDK

// FILE: some/HashMap.java

package some;

public class HashMap<K, V> extends java.util.HashMap<K, V> {}

// FILE: test.kt

import java.util.*
import some.*

// K1: misses both some.HashMap and java.util.HashMap due to ambiguous classifiers, takes kotlin.collections.HashMap from the next scope
// K2: works the same way currently. See logic in BodyResolveComponents.resolveRootPartOfQualifier
fun test() = HashMap::class
