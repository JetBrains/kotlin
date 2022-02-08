open <!REPEATED_MODIFIER!>open<!> class A

internal <!REPEATED_MODIFIER!>internal<!> object B

enum <!REPEATED_MODIFIER!>enum<!> class C {
    VALUE1, VALUE2;

    protected <!REPEATED_MODIFIER!>protected<!> companion object {
        private <!REPEATED_MODIFIER!>private<!> val D = 5
    }

    inline <!REPEATED_MODIFIER!>inline<!> fun foo(f: (Int) -> Int) = f(8)
}

open class E(private <!REPEATED_MODIFIER!>private<!> val int: Int = 5) {
    protected <!REPEATED_MODIFIER!>protected<!> var double = int + 8.0
}
