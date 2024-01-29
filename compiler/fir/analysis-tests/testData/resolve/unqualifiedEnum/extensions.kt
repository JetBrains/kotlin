// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

// FILE: Duration.kt

package Duration

data class Duration(val milliseconds: Int) {
    companion object {
        val ZERO: Duration = Duration(0)
        val Int.seconds: Duration get() = Duration(this * 1000)
        val Int.minutes: Duration get() = (this * 60).seconds
    }
}

val Duration.Companion.ONE_SECOND: Duration get() = Duration(1000)

// FILE: Test1.kt

import Duration.Duration

val z1: Duration = ZERO
val d1: Duration = 1.<!UNRESOLVED_REFERENCE!>seconds<!>
val o1: Duration = <!UNRESOLVED_REFERENCE!>ONE_SECOND<!>

// FILE: Test2.kt

import Duration.Duration
import Duration.ONE_SECOND

val z2: Duration = ZERO
val d2: Duration = 1.<!UNRESOLVED_REFERENCE!>seconds<!>
val o2: Duration = ONE_SECOND
