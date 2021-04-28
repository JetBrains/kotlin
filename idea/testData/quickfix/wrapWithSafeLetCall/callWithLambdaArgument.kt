// "Wrap with '?.let { ... }' call" "true"
// SHOULD_BE_AVAILABLE_AFTER_EXECUTION
// ERROR: Type mismatch: inferred type is String? but String was expected
// WITH_RUNTIME

interface Foo {
    val f: ((() -> Unit) -> String)?
}

fun test(foo: Foo) {
    bar(foo.<caret>f {})
}

fun bar(s: String) {}
