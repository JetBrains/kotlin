// FULL_JDK

import java.util.EnumMap

typealias SomeEnumMap = EnumMap<<!UPPER_BOUND_VIOLATED!>String<!>, String?>

fun test(map: <!UPPER_BOUND_VIOLATED_IN_TYPEALIAS_EXPANSION!>SomeEnumMap<!>, qualifier: String?) {
    val result = map[qualifier]
}
