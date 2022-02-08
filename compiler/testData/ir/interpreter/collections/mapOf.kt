import kotlin.*
import kotlin.collections.*

const val a = mapOf(1 to "1", 2 to "2", 3 to "3").<!EVALUATED: `3`!>size<!>
const val b = emptyMap<Any, Any>().<!EVALUATED: `0`!>size<!>

const val contains1 = mapOf(1 to "1", 2 to "2", 3 to "3").<!EVALUATED: `true`!>containsKey(1)<!>
const val contains2 = mapOf(1 to "1", 2 to "2", 3 to "3").<!EVALUATED: `true`!>contains(1)<!>
const val contains3 = mapOf(1 to "1", 2 to "2", 3 to "3").<!EVALUATED: `false`!>contains<Any, String>("1")<!>
const val contains4 = mapOf(1 to "1", 2 to "2", 3 to "3").<!EVALUATED: `true`!>containsValue("1")<!>

const val get1 = mapOf(1 to "1", 2 to "2", 3 to "3").get(1)<!EVALUATED: `1`!>!!<!>
const val get2 = mapOf(1 to "1", 2 to "2", 3 to "3")[2]<!EVALUATED: `2`!>!!<!>
const val get3 = mapOf(1 to "1", 2 to "2", 3 to "3")[0].<!EVALUATED: `null`!>toString()<!>

const val keys = mapOf(1 to "1", 2 to "2", 3 to "3").keys.<!EVALUATED: `3`!>size<!>
const val values = mapOf(1 to "1", 2 to "2", 3 to "3").values.<!EVALUATED: `3`!>size<!>
const val entries = mapOf(1 to "1", 2 to "2", 3 to "3").entries.<!EVALUATED: `3`!>size<!>
