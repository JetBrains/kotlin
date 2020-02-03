// !DIAGNOSTICS: -UNUSED_VARIABLE
// !WITH_NEW_INFERENCE
// FULL_JDK

import java.util.*

fun bar(): String? = null

fun fooHashSet() {
    var x = HashSet<String>()
    x.add(null)
    x.add(bar())
    x.add("")

    val b1: MutableSet<String?> = x
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = x
}

fun fooTreeSet() {
    var x = TreeSet<String>()
    x.add(null)
    x.add(bar())
    x.add("")

    val b1: MutableSet<String?> = x
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = x
}

fun fooLinkedHashSet() {
    var x = LinkedHashSet<String>()
    x.add(null)
    x.add(bar())
    x.add("")

    val b1: MutableSet<String?> = x
    val b2: MutableSet<String> = x
    val b3: Set<String?> = x

    val b4: Collection<String?> = x
    val b6: MutableCollection<String?> = x
}
