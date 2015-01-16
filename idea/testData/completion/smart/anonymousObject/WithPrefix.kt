trait Foo

var a : Foo = o<caret>

// EXIST: { lookupString:"object", itemText:"object: Foo{...}" }
