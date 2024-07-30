// WITH_STDLIB
val foo = 1.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>

typealias IntAlias = Int
val aliased: IntAlias = 1

val bar = aliased.<!REDUNDANT_CALL_OF_CONVERSION_METHOD!>toInt()<!>
