class A

<!WRONG_EXTERNAL_DECLARATION!>external fun A.foo(): Unit<!> = definedExternally

<!WRONG_EXTERNAL_DECLARATION!>external var A.bar: String<!>
    get() = definedExternally
    set(value) = definedExternally

<!WRONG_EXTERNAL_DECLARATION!>external val A.baz: String<!>
    get() = definedExternally