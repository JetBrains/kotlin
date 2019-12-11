// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo() {
    object a {}
    val b = object {
        object c {}
    }
    b.c
    class A {
        object d {}
    }
    val f = {
        object e {}
    }
}