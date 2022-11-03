// FIR_DISABLE_LAZY_RESOLVE_CHECKS
// FULL_JDK

import java.util.EnumMap

typealias SomeEnumMap = EnumMap<<!UPPER_BOUND_VIOLATED!>String<!>, String?>

fun test(map: SomeEnumMap, qualifier: String?) {
    val result = map[qualifier]
}
