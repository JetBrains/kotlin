interface Your

class My {
    internal val x = object : Your {}

    internal fun foo() = <!UNRESOLVED_REFERENCE!>{
        class Local
        Local()
    }()<!>
}