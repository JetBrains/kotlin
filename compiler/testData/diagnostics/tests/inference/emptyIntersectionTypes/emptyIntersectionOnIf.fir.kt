// ISSUE: KT-57649

open class A
abstract class B {
    fun test(current: A): A? =
        // K2 reports empty intersection here due to the smart cast from A to B, where A & B aren't compatible
        <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_ERROR("K; B?, A?; multiple incompatible classes; : B, A")!>if (current === this) current else null<!>
}
