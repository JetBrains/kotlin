// WITH_REFLECT
// LANGUAGE: -PrioritizedEnumEntries
// ISSUE: KT-58922

import kotlin.reflect.*
import kotlin.enums.*

enum class E {
    ;

    val entries: Int = 0
}

fun test() {
    // K1 warning (false, resolve will never change here)
    val x: KProperty0<EnumEntries<E>> = E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
    // K1 warning (false, resolve will never change here)
    val y: KProperty1<E, Int> = E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
    // No warning
    val xx: () -> EnumEntries<E> = E::entries
    // No warning
    val yy: (E) -> Int = E::entries
    // K1 warning (Ok)
    val z = E::<!DEPRECATED_ACCESS_TO_ENUM_ENTRY_PROPERTY_AS_REFERENCE!>entries<!>
}
