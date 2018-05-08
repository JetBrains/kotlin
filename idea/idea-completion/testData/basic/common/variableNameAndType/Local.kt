
class Foo
class BarFoo

fun test() {
    var f<caret>
}

// EXIST: { itemText: "foo", tailText: ": Foo (<root>)" }
// ABSENT: { itemText: "foo", tailText: ": BarFoo (<root>)" }