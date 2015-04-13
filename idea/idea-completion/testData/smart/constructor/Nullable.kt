class Foo

fun foo(p : Any){
    var a : Foo? = <caret>
}

// EXIST: { lookupString:"Foo", itemText:"Foo", tailText: "() (<root>)" }
