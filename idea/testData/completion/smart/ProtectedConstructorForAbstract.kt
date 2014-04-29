abstract class Foo protected()

var a : Foo = <caret>

// ABSENT: Foo
// EXIST: { lookupString:"object", itemText:"object: Foo(){...}" }
