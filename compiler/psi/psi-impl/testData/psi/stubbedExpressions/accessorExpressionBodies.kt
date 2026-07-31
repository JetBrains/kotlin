// The expression body of a property accessor is absent from the stub tree
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
