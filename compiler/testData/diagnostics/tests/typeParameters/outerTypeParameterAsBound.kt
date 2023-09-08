// FIR_IDENTICAL
// ISSUE: KT-61959

package test

interface OuterParam

class Outer<OuterParam> {
    class Nested<NestedParam : OuterParam>
}

fun main() {
    Outer.Nested<OuterParam>()
}
