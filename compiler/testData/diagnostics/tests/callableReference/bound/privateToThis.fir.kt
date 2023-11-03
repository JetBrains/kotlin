// !DIAGNOSTICS: -UNUSED_VARIABLE

class Foo<out T>(name: T) {
    private var prop: T = name
        private set

    fun testProp() {
        val ok1 = this::prop
        val ok2 = this@Foo::prop
        val ok3 = object { val y: Any = this@Foo::prop }

        val fail1 = Foo(prop)::<!INVISIBLE_REFERENCE!>prop<!>
    }

    fun testFunc() {
        val ok1 = this::func
        val ok2 = this@Foo::func
        val ok3 = object { val y: Any = this@Foo::func }

        val fail1 = Foo(prop)::<!INVISIBLE_REFERENCE!>func<!>
    }

    private fun func(t: T): T = t
}
