import wrong.*

fun foo(x: ClassWithWrongAbiVersion) {
    bar()

    1.printStackTrace(2, 3)
}
