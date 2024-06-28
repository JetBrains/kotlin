// FIR_IDENTICAL
// ISSUE: KT-56520 (case 7)
// FIR_DUMP
// JDK_KIND: FULL_JDK_17
// DIAGNOSTICS: -PLATFORM_CLASS_MAPPED_TO_KOTLIN

// FILE: some/Map.kt

package some

class Map {
    companion object {
        fun of() = 42
    }
}

// FILE: other/Map.kt

package other

class Map

// FILE: test.kt

import java.util.*
import some.*

// K1: misses both some.Map and java.util.Map due to ambiguous classifiers, takes kotlin.collections.Map from the next scope
// K2: works the same way currently. See logic in BodyResolveComponents.resolveRootPartOfQualifier
// (looks like kotlin.collections.Map.of is not available)
fun test() = Map.<!UNRESOLVED_REFERENCE!>of<!>()

// FILE: test2.kt

import java.util.*
import other.*

// K1: misses both some.Map and java.util.Map due to ambiguous classifiers, takes kotlin.collections.Map from the next scope
// K2: works the same way currently. See logic in BodyResolveComponents.resolveRootPartOfQualifier
fun test2() = Map.<!UNRESOLVED_REFERENCE!>of<!>()

// FILE: test3.kt

import java.util.Map

fun test3() = Map.of<String, String>()

// FILE: test4.kt

import some.Map

fun test4() = Map.of()
