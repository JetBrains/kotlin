// !DIAGNOSTICS: -UNUSED_PARAMETER -PARAMETER_NAME_CHANGED_ON_OVERRIDE
// FULL_JDK

class KotlinMap1<K, V> : java.util.AbstractMap<K, V>() {
    override val entries: MutableSet<MutableMap.MutableEntry<K, V>>
        get() = throw UnsupportedOperationException()

    override fun remove(x: K, y: V) = true
}

class KotlinMap2 : java.util.AbstractMap<String, Int>() {
    override val entries: MutableSet<MutableMap.MutableEntry<String, Int>>
        get() = throw UnsupportedOperationException()

    override fun remove(x: String, y: Int) = true
}

fun foo(x: MutableMap<String, Int>, y: java.util.HashMap<String, Int>, z: java.util.AbstractMap<String, Int>) {
    x.remove("", 1)
    x.remove("", <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    x.remove("", <!NULL_FOR_NONNULL_TYPE!>null<!>)

    y.remove("", 1)
    y.remove("", <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    y.remove("", <!NULL_FOR_NONNULL_TYPE!>null<!>)

    z.remove("", 1)
    z.remove("", <!ARGUMENT_TYPE_MISMATCH!>""<!>)
    z.remove("", null)
}
