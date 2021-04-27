// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.*

fun bar(): String? = null
val nullableInt: Int? = null

fun hashMapTest() {
    var x: HashMap<String, Int?> = HashMap<String, Int?>()
    x.put(null, null)
    x.put("", null)
    x.put(bar(), 1)
    x.put("", 1)

    x[null] = 1
    x[bar()] = 1
    x[""] = nullableInt
    x[""] = 1

    val b1: MutableMap<String, Int> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String, Int?> = x
    val b3: Map<String, Int> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
    val b4: Map<String, Int?> = x
    val b5: Map<String?, Int?> = x

    val b6: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}

fun treeMapTest() {
    var x: TreeMap<String, Int?> = TreeMap<String, Int?>()
    x.put(null, null)
    x.put("", null)
    x.put(bar(), 1)
    x.put("", 1)

    x[null] = 1
    x[bar()] = 1
    x[""] = nullableInt
    x[""] = 1

    val b1: MutableMap<String, Int> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String, Int?> = x
    val b3: Map<String, Int> = <!INITIALIZER_TYPE_MISMATCH!>x<!>
    val b4: Map<String, Int?> = x
    val b5: Map<String?, Int?> = x

    val b6: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!INITIALIZER_TYPE_MISMATCH, TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}
