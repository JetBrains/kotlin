// TYPE_MAPPING_MODE: DEFAULT

class Foo<in T>

fun test() {
    val l<caret>ocal: Foo<CharSequence> = Foo()
}