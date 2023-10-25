// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_ANONYMOUS_PARAMETER -UNUSED_PARAMETER, -UNREACHABLE_CODE

val prop: String = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>

val prop2: String
    get() = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>

fun foo(x: Int, y: String = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>) {
    println("Hello")
    println("world")

    object {
        fun bar(): Any = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>
    }

    listOf<String>()
            .map<String, String> { <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!> }
            .filter(fun(x: String): Boolean { <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!> })

    <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>
}

open class A(val x: Int)

open class B() : A(<!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>) {
    constructor(y: String) : this()

    constructor(y: String, z: String) : this(y + z + <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>)
}

private fun exposeClassS() = run {
    class S {
        inner class W {
            val v: String = <!CALL_TO_DEFINED_EXTERNALLY_FROM_NON_EXTERNAL_DECLARATION!>definedExternally<!>
        }
    }
    S()
}
