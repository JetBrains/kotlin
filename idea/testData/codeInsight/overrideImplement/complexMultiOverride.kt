// FIR_IDENTICAL
open class Base<A, B, C>() {
    open val method : (A?) -> A = { it!! }
    open fun foo(value : B) : B = value
    open fun bar(value : () -> C) : (String) -> C = { value() }
}

class C : Base<String, C, Unit>() {
    <caret>
}
