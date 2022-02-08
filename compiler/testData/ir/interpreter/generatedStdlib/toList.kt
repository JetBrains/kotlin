import kotlin.*
import kotlin.collections.*
import kotlin.text.*
import kotlin.sequences.*

const val zeroElementIntArrayToList = intArrayOf().toList().<!EVALUATED: `0`!>size<!>
const val singleElementIntArrayToList = intArrayOf(1).toList().<!EVALUATED: `1`!>size<!>
const val intArrayToList = intArrayOf(1, 2, 3).toList().<!EVALUATED: `3`!>size<!>
const val customArrayToList = <!EVALUATED: `Some other value`!>arrayOf(1, "2", 3.0, "Some other value").toList()[3] as String<!>

const val listFromSet = setOf(1, 2, 2, 3, 3).toList().<!EVALUATED: `1, 2, 3`!>joinToString()<!>
const val listFromIterable = mapOf(1 to "One", 2 to "Two", 3 to "Three").entries.toList().<!EVALUATED: `1=One, 2=Two, 3=Three`!>joinToString()<!>

const val stringToList = "String".toList().<!EVALUATED: `S, t, r, i, n, g`!>joinToString()<!>

const val sequenceToList = sequenceOf(3, 2, 1).toList().<!EVALUATED: `3, 2, 1`!>joinToString()<!>
