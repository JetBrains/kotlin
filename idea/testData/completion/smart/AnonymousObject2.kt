abstract class Foo

var a : Foo = <caret>

// ABSENT: Foo
// EXIST: { lookupString:"object", itemText:"object: Foo(){...}" }
