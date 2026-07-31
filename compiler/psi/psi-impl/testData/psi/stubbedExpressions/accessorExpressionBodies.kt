// The expression body of a property accessor is stubbed as long as the expression itself is stub-based
package test

val getter: Int
    get() = 1

val inlineGetter: String get() = "s"

var withBothAccessors: Int
    get() = 2
    set(value) {}

class WithMember {
    val member: Char
        get() = 'm'
}
