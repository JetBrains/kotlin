import kotlin.sequences.*

const val a = <!EVALUATED: `1`!>sequenceOf(1, 2, 3).iterator().next()<!>
const val b = <!EVALUATED: `2`!>sequenceOf(2, 3).iterator().next()<!>
const val c = <!EVALUATED: `false`!>sequenceOf<Int>().iterator().hasNext()<!>
const val d = <!EVALUATED: `42`!>generateSequence() { 42 }.iterator().next()<!>
