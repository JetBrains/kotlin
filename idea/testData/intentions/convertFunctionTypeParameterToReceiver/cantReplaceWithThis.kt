// SHOULD_FAIL_WITH: Following expression won't be processed since refactoring can't preserve its semantics: this
val o = object {
    fun bar() {
        foo { i, b -> "$this: $i $b" }
    }
}

fun foo(f: (<caret>Int, Boolean) -> String) {

}