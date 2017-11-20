
class Foo

fun test() {
    var f<caret>
}

// EXIST: { itemText: "foo", tailText: ": Foo (<root>)" }