// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    interface a {}
    val b = object {
        interface c {}
    }
    class A {
        interface d {}
    }
    val f = {
        interface e {}
    }
}