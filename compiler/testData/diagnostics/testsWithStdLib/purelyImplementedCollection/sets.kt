// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.*

fun bar(): String? = null

fun fooHashSet() {
    var x = HashSet<String>()
    x.add(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.add(<!TYPE_MISMATCH!>bar()<!>)
    x.add("")

    val b1: MutableSet<String?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = <!TYPE_MISMATCH!>x<!>
}

fun fooTreeSet() {
    var x = TreeSet<String>()
    x.add(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.add(<!TYPE_MISMATCH!>bar()<!>)
    x.add("")

    val b1: MutableSet<String?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = <!TYPE_MISMATCH!>x<!>
}

fun fooLinkedHashSet() {
    var x = LinkedHashSet<String>()
    x.add(<!NULL_FOR_NONNULL_TYPE!>null<!>)
    x.add(<!TYPE_MISMATCH!>bar()<!>)
    x.add("")

    val b1: MutableSet<String?> = <!TYPE_MISMATCH!>x<!>
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = <!TYPE_MISMATCH!>x<!>
}
