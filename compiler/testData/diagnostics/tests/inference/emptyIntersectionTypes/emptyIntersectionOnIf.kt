// ISSUE: KT-57649

open class A
abstract class B {
    fun test(current: A): A? =
        // K2 reports empty intersection here due to the smart cast from A to B, where A & B aren't compatible
        if (current === this) current else null
}
