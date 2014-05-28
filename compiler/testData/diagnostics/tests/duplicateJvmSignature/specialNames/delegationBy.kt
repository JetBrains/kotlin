trait Foo

class Bar(f: Foo) : Foo by f {
    <!CONFLICTING_JVM_DECLARATIONS!>val `$delegate_0`: Foo?<!> = null
}