external fun foo(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>f: Int.() -> Int<!>)

external fun bar(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>vararg f: Int.() -> Int<!>)

<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>external fun baz(): Int.() -> Int<!>

<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>external val prop: Int.() -> Int<!>

<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>external var prop2: Int.() -> Int<!>

external val propGet
    <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>get(): Int.() -> Int<!> = definedExternally

external var propSet
    <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>get(): Int.() -> Int<!> = definedExternally
    set(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>v: Int.() -> Int<!>) = definedExternally

external class A(<!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>f: Int.() -> Int<!>)

external data class <!WRONG_EXTERNAL_DECLARATION!>B(
        <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>val a: Int.() -> Int<!>,
        <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION, EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>var b: Int.() -> Int<!>
)<!> {
    <!EXTENSION_FUNCTION_IN_EXTERNAL_DECLARATION!>val c: Int.() -> Int<!>
}