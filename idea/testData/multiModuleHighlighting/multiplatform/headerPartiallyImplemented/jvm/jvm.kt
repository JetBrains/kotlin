impl class <error>My</error> {

    <error>impl fun foo()</error> = 42
}

impl class <error>Your</error> {

    <error>impl fun foo()</error> = 13

    <error>impl fun bar(arg: Int)</error> = arg

}

impl class His {

    impl fun foo() = 7

    impl fun bar(arg: Int) = arg == foo()

}
