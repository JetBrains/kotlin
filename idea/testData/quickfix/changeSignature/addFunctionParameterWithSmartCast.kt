// "Add parameter to function 'fooFun'" "true"
sealed class Foo {
    class SubFoo : Foo()
    class Sub2Foo : Foo()
}

fun fooFun(): Int = 1

val subFoo: Foo = Foo.SubFoo()
val bar = when(subFoo){
    is Foo.SubFoo -> fooFun(<caret>subFoo)
    else -> 0
}