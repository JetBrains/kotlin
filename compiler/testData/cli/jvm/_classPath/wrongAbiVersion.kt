import wrong.*

fun foo(x: ClassWithWrongAbiVersion) {
    bar()

    1.replaceIndent(2, 3)
}
