// TYPE_MAPPING_MODE: RETURN_TYPE

class Foo<T>

fun t<caret>est(): Foo<CharSequence> {
    return Foo<CharSequence>()
}