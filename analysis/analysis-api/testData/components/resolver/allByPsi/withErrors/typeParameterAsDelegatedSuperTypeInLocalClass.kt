package bar

interface MyInterface

fun <T : MyInterface> foo(i: T) {
    class Local : T by i

    object : T by i {}
}
