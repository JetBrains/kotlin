import kotlin.*
import kotlin.collections.*

const val a = <!EVALUATED: `3`!>mapOf(1 to "1", 2 to "2", 3 to "3").size<!>
const val b = <!EVALUATED: `0`!>emptyMap<Any, Any>().size<!>

const val contains1 = <!EVALUATED: `true`!>mapOf(1 to "1", 2 to "2", 3 to "3").containsKey(1)<!>
const val contains2 = <!EVALUATED: `true`!>mapOf(1 to "1", 2 to "2", 3 to "3").contains(1)<!>
const val contains3 = <!EVALUATED: `false`!>mapOf(1 to "1", 2 to "2", 3 to "3").contains<Any, String>("1")<!>
const val contains4 = <!EVALUATED: `true`!>mapOf(1 to "1", 2 to "2", 3 to "3").containsValue("1")<!>

const val get1 = <!EVALUATED: `1`!>mapOf(1 to "1", 2 to "2", 3 to "3").get(1)!!<!>
const val get2 = <!EVALUATED: `2`!>mapOf(1 to "1", 2 to "2", 3 to "3")[2]!!<!>
const val get3 = <!EVALUATED: `null`!>mapOf(1 to "1", 2 to "2", 3 to "3")[0].toString()<!>

const val keys = <!EVALUATED: `3`!>mapOf(1 to "1", 2 to "2", 3 to "3").keys.size<!>
const val values = <!EVALUATED: `3`!>mapOf(1 to "1", 2 to "2", 3 to "3").values.size<!>
const val entries = <!EVALUATED: `3`!>mapOf(1 to "1", 2 to "2", 3 to "3").entries.size<!>
