// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
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