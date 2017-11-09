class Foo<T, V>

class Bar: Foo<S<caret>

// ELEMENT: StringBuilder
// TAIL_TEXT: " (kotlin.text)"