class Foo<T>

fun foo(p : Any){
    var a : Foo<String> = <caret>
}

// EXIST: { lookupString:"Foo", itemText:"Foo<String>", tailText:"() (<root>)" }
