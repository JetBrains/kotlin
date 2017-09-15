actual class <error>My</error> {

    actual fun foo() = 42
}

actual class Your {

    actual fun foo() = 13

    <error>actual fun bar(arg: Int)</error> = arg

}

actual class His {

    actual fun foo() = 7

    actual fun bar(arg: Int) = arg == foo()

}
