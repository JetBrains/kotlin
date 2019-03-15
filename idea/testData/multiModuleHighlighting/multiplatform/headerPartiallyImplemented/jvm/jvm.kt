package foo

actual class <error>My</error> {

    actual fun foo() = 42
}

actual class <error>Your</error> {

    actual fun foo() = 13

    actual fun <error>bar</error>(arg: Int) = arg

}

actual class His {

    actual fun foo() = 7

    actual fun bar(arg: Int) = arg == foo()

}
