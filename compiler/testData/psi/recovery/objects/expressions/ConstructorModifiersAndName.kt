
val foo = object Name private () {}

val foo = object Name private () : Bar {

}

val foo = object Name @[foo] private @[bar()] () {}

val foo = object Name private ()
