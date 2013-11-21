class Foo<T>

fun foo(p : Any){
    var a : Foo<String> = <caret>
}

// EXIST: Foo@Foo<jet.String>()
