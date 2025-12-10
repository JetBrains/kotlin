// TYPE_MAPPING_MODE: VALUE_PARAMETER

class Foo<in T>

context(ct<caret>x: Foo<String>)
fun test() {}