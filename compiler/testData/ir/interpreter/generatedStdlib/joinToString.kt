import kotlin.collections.*
import kotlin.sequences.*

const val a = <!EVALUATED: `1, 2, 3`!>listOf(1, 2, 3).joinToString()<!>
const val b = <!EVALUATED: `-1.-2.-3`!>sequenceOf(-1, -2, -3).joinToString(separator = ".")<!>
