// LANGUAGE: +ContextParameters
// TYPE_MAPPING_MODE: VALUE_PARAMETER

// WITH_STDLIB
// FULL_JDK
// RENDER_CLASS_DUMP

class Foo<T>

context(ct<caret>x: Foo<CharSequence>)
fun test() {}