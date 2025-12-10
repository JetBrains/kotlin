// TYPE_MAPPING_MODE: DEFAULT

class Foo<T>

fun test() {
    val l<caret>ocal: Foo<CharSequence> = Foo()
}