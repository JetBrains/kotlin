import kotlin.collections.*
import kotlin.sequences.*

const val a = listOf(1, 2, 3).<!EVALUATED: `1, 2, 3`!>joinToString()<!>
const val b = sequenceOf(-1, -2, -3).<!EVALUATED: `-1.-2.-3`!>joinToString(separator = ".")<!>
