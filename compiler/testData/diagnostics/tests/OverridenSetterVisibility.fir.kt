public interface ITest {
    public var prop : Int
        get() = 12
        set(value) {}
}

abstract class ATest {
    protected open var prop2 : Int
        get() = 13
        set(value) {}
}

class Test: ATest(), ITest {
    override var prop : Int
        get() = 12
        private set(value) {}

    override var prop2 : Int
        get() = 14
        <!SETTER_VISIBILITY_INCONSISTENT_WITH_PROPERTY_VISIBILITY!>internal<!> set(value) {}
}

fun main() {
    val test = Test()
    <!INVISIBLE_SETTER!>test.prop<!> = 12

    val itest: ITest = test
    itest.prop = 12 // No error here
}
