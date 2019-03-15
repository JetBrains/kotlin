
class Foo
class BarFoo

val f<caret>

// EXIST: { itemText: "foo", tailText: ": Foo (<root>)" }
// ABSENT: { itemText: "foo", tailText: ": BarFoo (<root>)" }