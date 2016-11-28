class A

<!WRONG_EXTERNAL_DECLARATION!>external fun A.foo(): Unit<!> = noImpl

<!WRONG_EXTERNAL_DECLARATION!>external var A.bar: String<!>
    get() = noImpl
    set(value) = noImpl

<!WRONG_EXTERNAL_DECLARATION!>external val A.baz: String<!>
    get() = noImpl