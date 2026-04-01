// TYPE_MAPPING_MODE: RETURN_TYPE

// WITH_STDLIB
// FULL_JDK
// RENDER_CLASS_DUMP

class Foo<T>

fun t<caret>est(): Foo<CharSequence> {
    return Foo<CharSequence>()
}