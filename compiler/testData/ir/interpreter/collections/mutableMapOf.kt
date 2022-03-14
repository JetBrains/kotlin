import kotlin.*
import kotlin.collections.*

const val a1 = <!EVALUATED: `3`!>mutableMapOf(1 to "1", 2 to "2", 3 to "3").size<!>
const val a2 = <!EVALUATED: `2`!>mutableMapOf(1 to "1", 2 to "2", 3 to "3").apply { remove(1) }.size<!>

