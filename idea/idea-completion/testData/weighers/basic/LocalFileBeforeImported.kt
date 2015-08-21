package test

import some.fooImported

val fooVar = 12
fun fooCurentFile() = 12

val some = foo<caret>

// "foo" is before other elements because of exact prefix match

// ORDER: foo
// ORDER: fooVar
// ORDER: fooCurentFile
// ORDER: fooFromSamePackage
// ORDER: fooImported
// ORDER: fooNotImported
// INVOCATION_COUNT: 2
// SELECTED: 0