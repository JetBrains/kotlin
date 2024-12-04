import kotlin.*
import kotlin.collections.*
import kotlin.text.*
import kotlin.sequences.*

const val zeroElementIntArrayToList = <!EVALUATED: `0`!>intArrayOf().toList().size<!>
const val singleElementIntArrayToList = <!EVALUATED: `1`!>intArrayOf(1).toList().size<!>
const val intArrayToList = <!EVALUATED: `3`!>intArrayOf(1, 2, 3).toList().size<!>
const val customArrayToList = <!EVALUATED: `Some other value`!>arrayOf(1, "2", 3.0, "Some other value").toList()[3] as String<!>

const val listFromSet = <!EVALUATED: `1, 2, 3`!>setOf(1, 2, 2, 3, 3).toList().joinToString()<!>
const val listFromIterable = <!EVALUATED: `1=One, 2=Two, 3=Three`!>mapOf(1 to "One", 2 to "Two", 3 to "Three").entries.toList().joinToString()<!>

const val stringToList = <!EVALUATED: `S, t, r, i, n, g`!>"String".toList().joinToString()<!>

const val sequenceToList = <!EVALUATED: `3, 2, 1`!>sequenceOf(3, 2, 1).toList().joinToString()<!>
