// KT-880 Overload resolution ambiguity

public interface I {
    <!REDUNDANT_OPEN_IN_INTERFACE!>open<!> fun test() : Unit
}

abstract public class A() {
    open public fun test() : Unit {
    }
}

public open class T() : A(), I {
    open fun main() : Unit {
        test() // Test no "Overload resolution ambiguity" is here
    }
}