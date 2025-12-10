// TYPE_MAPPING_MODE: VALUE_PARAMETER

class Foo<out T>

context(ct<caret>x: Foo<String>)
fun test() {}