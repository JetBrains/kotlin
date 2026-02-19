// TYPE_MAPPING_MODE: DEFAULT

class Foo<out T>

fun test() {
    val l<caret>ocal: Foo<String> = Foo()
}