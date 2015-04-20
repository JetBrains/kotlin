// !CHECK_TYPE

import kotlin.reflect.*

val String.countCharacters: Int
    get() = length()

var Int.meaning: Long
    get() = 42L
    set(value) {}

fun test() {
    val f = String::countCharacters
    
    checkSubtype<KTopLevelExtensionProperty<String, Int>>(f)
    checkSubtype<KExtensionProperty<String, Int>>(f)
    checkSubtype<KMutableExtensionProperty<String, Int>>(<!TYPE_MISMATCH!>f<!>)
    checkSubtype<Int>(f.get("abc"))
    f.<!UNRESOLVED_REFERENCE!>set<!>("abc", 0)

    val g = Int::meaning

    checkSubtype<KTopLevelExtensionProperty<Int, Long>>(g)
    checkSubtype<KExtensionProperty<Int, Long>>(g)
    checkSubtype<KMutableTopLevelExtensionProperty<Int, Long>>(g)
    checkSubtype<KMutableExtensionProperty<Int, Long>>(g)
    checkSubtype<Long>(g.get(0))
    g.set(1, 0L)
}
