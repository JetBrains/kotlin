// !DIAGNOSTICS: -UNUSED_PARAMETER, -UNREACHABLE_CODE

val prop: String = <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>

val prop2: String
    get() = <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>

fun foo(x: Int, y: String = <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>) {
    println("Hello")
    println("world")

    object {
        fun bar(): Any = <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>
    }

    listOf<String>()
            .map<String, String> { <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!> }
            .filter(fun(x: String): Boolean { <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!> })

    <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>
}

open class A(val x: Int)

open class B() : A(<!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>) {
    constructor(y: String) : this()

    constructor(y: String, z: String) : this(y + z + <!CALL_TO_NO_IMPL_FROM_NON_EXTERNAL_DECLARATION!>noImpl<!>)
}
