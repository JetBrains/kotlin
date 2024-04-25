// FIR_IDENTICAL
// SKIP_TXT

/**
 * KDoc for Foo1
 */
class Foo1() {}

public class Foo2() {
    /**
     * KDoc for method
     */
    fun method() {}

    /**
     * KDoc for method2
     */
    public fun method2() {}
    private fun method3() {}

    fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>implicit<!>() = 10
    public fun <!NO_EXPLICIT_RETURN_TYPE_IN_API_MODE!>implicit2<!>() = 10
    public fun implicit3(): Int = 10
}

public open class ClassWithOpen() {
    /**
     * KDoc for method
     */
    fun method() {}

    /**
     * KDoc for openMethod
     */
    open fun openMethod() {}
}

public data class FooData(val i: Int, val s: String)

data class FooData2(val i: Int, val s: String)

public class WithNested {
    class Nested {}
    inner class Inner {}
}

enum class Foo { A, B }
public enum class Bar { A, B }
