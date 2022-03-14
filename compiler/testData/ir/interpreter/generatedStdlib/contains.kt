import kotlin.collections.*
import kotlin.sequences.*

const val a = <!EVALUATED: `true`!>intArrayOf(1, 2, 3).contains(1)<!>
const val b = <!EVALUATED: `false`!>intArrayOf(1, 2, 3).contains(4)<!>
const val c = <!EVALUATED: `true`!>arrayOf(1, "2", 3.0).contains("2")<!>
const val d = <!EVALUATED: `true`!>sequenceOf(1, 2, 3).contains(2)<!>
