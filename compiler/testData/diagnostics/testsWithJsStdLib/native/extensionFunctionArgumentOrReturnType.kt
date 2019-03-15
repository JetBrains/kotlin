external fun foo(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>f: Int.() -> Int<!>)

external fun bar(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>vararg f: Int.() -> Int<!>)

external fun baz(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external val prop: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external var prop2: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>

external val propGet
    get(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!> = definedExternally

external var propSet
    get(): <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!> = definedExternally
    set(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>v: Int.() -> Int<!>) = definedExternally

external class A(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>f: Int.() -> Int<!>)

external data class <!WRONG_EXTERNAL_DECLARATION!>B(
        <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>val a: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!><!>,
        <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>var b: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!><!>
)<!> {
    val c: <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>Int.() -> Int<!>
}