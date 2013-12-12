package sample

enum class Foo {
    X
    Y
}

fun foo(){
    val f : Foo = <caret>
}

// EXIST: { lookupString:"Foo.X", itemText:"Foo.X", tailText:" (sample)", typeText:"sample.Foo" }
// EXIST: { lookupString:"Foo.Y", itemText:"Foo.Y", tailText:" (sample)", typeText:"sample.Foo" }
