class Foo<T>

fun foo(p : Object){
    var a : Foo<String> = <caret>
}

// EXIST: Foo@Foo<jet.String>()
