// !API_VERSION: 1.3
// !JVM_TARGET: 1.8
interface A {
    <!JVM_DEFAULT_IN_DECLARATION!>@JvmDefault
    fun test()<!> {
    }
}

interface <!JVM_DEFAULT_THROUGH_INHERITANCE!>B<!> : A {

}


open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo<!> : B
open class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Foo2<!> : B, A

class Bar : Foo()
class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Bar2<!> : Foo(), A
class <!JVM_DEFAULT_THROUGH_INHERITANCE!>Bar3<!> : Foo(), B