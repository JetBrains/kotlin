impl class <error>My</error> {

    impl fun foo() = 42
}

impl class Your {

    impl fun foo() = 13

    <error>impl fun bar(arg: Int)</error> = arg

}

impl class His {

    impl fun foo() = 7

    impl fun bar(arg: Int) = arg == foo()

}
