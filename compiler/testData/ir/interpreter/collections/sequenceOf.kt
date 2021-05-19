import kotlin.sequences.*

const val a = sequenceOf(1, 2, 3).iterator().<!EVALUATED: `1`!>next()<!>
const val b = sequenceOf(2, 3).iterator().<!EVALUATED: `2`!>next()<!>
const val c = sequenceOf<Int>().iterator().<!EVALUATED: `false`!>hasNext()<!>
const val d = generateSequence() { 42 }.iterator().<!EVALUATED: `42`!>next()<!>
