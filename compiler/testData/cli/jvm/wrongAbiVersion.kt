import wrong.*

fun foo(x: ClassWithWrongAbiVersion) {
    bar()

    1.set(2, 3)
}
