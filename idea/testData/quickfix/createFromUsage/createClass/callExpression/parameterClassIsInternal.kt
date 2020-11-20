// "Create class 'Bar'" "true"
// DISABLE-ERRORS
internal class Foo

val bar = <caret>Bar(Foo())
