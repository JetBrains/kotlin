import kotlin.collections.*
import kotlin.sequences.*

const val a = intArrayOf(1, 2, 3).<!EVALUATED: `true`!>contains(1)<!>
const val b = intArrayOf(1, 2, 3).<!EVALUATED: `false`!>contains(4)<!>
const val c = arrayOf(1, "2", 3.0).<!EVALUATED: `true`!>contains("2")<!>
const val d = sequenceOf(1, 2, 3).<!EVALUATED: `true`!>contains(2)<!>
