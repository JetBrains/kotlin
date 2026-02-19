// RUN_PIPELINE_TILL: FRONTEND
external fun foo(f: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int)

external fun bar(vararg f: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int)

external fun baz(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external val prop: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external var prop2: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external val propGet
    get(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!> = definedExternally

external var propSet
    get(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!> = definedExternally
    set(v: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int) = definedExternally

external class A(f: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int)

external data class <!WRONG_EXTERNAL_DECLARATION!>B(
        val a: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!><!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int<!>,
        var b: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!><!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int<!>.() -> Int<!>
)<!> {
    val c: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>
}
