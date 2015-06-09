fun foo() {
    val foo = object private ()

    val foo = object private () : Bar

    val foo = object @[foo] private @[bar()] ()

    val foo = object private ()
}