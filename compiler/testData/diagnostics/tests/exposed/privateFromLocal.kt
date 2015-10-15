interface Your

class My {
    // private from local: ???
    private val x = object : Your {}

    // private from local: ???
    private fun foo() = {
        class Local
        Local()
    }()
}