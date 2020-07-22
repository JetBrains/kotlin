package p

public fun foo(a: Any) {
    a is <!OTHER_ERROR!>Map<Int><!>
    a is Map
    a is Map<out Any?, Any?>
    a is Map<*, *>
    a is Map<<!SYNTAX!><!>>
    a is List<Map>
    a is List
    a is Int

    (a as Map) is Int
}