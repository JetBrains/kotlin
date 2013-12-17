import some.fooImported

val fooVar = 12
fun fooLocal() = 12

val some = foo<caret>

// "foo" is before other elements because of exact prefix match

// ORDER: foo, fooVar, fooLocal, fooImported, fooNotImported
// INVOCATION_COUNT: 2
// SELECTED: 0