interface Your

class My {
    // private from local: ???
    private val x = object : Your {}

    // private from local: ???
    private fun foo() = <!UNRESOLVED_REFERENCE!>{
        class Local
        Local()
    }()<!>
}