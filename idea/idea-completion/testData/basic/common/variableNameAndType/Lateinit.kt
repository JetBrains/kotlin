
class Foo
class BarFoo

lateinit var f<caret>

// EXIST: { itemText: "foo: Foo" }
// EXIST: { itemText: "foo: BarFoo" }