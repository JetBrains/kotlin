// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.*

fun bar(): String? = null
val nullableInt: Int? = null

fun hashMapTest() {
    var x: HashMap<String?, Int> = HashMap<String?, Int>()
    x.put(null, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put(bar(), 1)
    x.put("", 1)

    x[null] = 1
    x[bar()] = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[""]<!> = <!NI;TYPE_MISMATCH!>nullableInt<!>
    x[""] = 1

    val b1: MutableMap<String?, Int?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String?, Int> = x
    val b3: Map<String?, Int> = x
    val b4: Map<String?, Int?> = x
    val b5: Map<String, Int?> = <!TYPE_MISMATCH!>x<!>

    val b6: Int = <!TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!TYPE_MISMATCH!>x[null]<!>
    val b8: Int = <!TYPE_MISMATCH!>x.get("")<!>

    val b9: Int? = x.get("")
}

fun treeMapTest() {
    var x: TreeMap<String?, Int> = TreeMap<String?, Int>()
    x.put(null, <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put("", <!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.put(bar(), 1)
    x.put("", 1)

    x[null] = 1
    x[bar()] = 1
    <!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>x[""]<!> = <!NI;TYPE_MISMATCH!>nullableInt<!>
    x[""] = 1

    val b1: MutableMap<String?, Int?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableMap<String?, Int> = x
    val b3: Map<String?, Int> = x
    val b4: Map<String?, Int?> = x
    val b5: Map<String, Int?> = <!TYPE_MISMATCH!>x<!>

    val b6: Int = <!TYPE_MISMATCH!>x[""]<!>
    val b7: Int = <!TYPE_MISMATCH!>x.get("")<!>

    val b8: Int? = x.get("")
}
