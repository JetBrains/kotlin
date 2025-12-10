// TYPE_MAPPING_MODE: RETURN_TYPE

class Foo<in T>

val tes<caret>t: Foo<String>
    get() = Foo<String>()