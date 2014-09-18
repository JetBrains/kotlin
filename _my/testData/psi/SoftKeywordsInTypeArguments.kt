class Foo<out abstract, out out> {}

fun f() {

//  Foo<out out>
  Foo<out Int>

}

