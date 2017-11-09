// SHOULD_FAIL_WITH: Following expression won't be processed since refactoring can't preserve its semantics: lambda()
open class Foo(f: (Int, <caret>Boolean) -> String)

fun lambda(): (Int, Boolean) -> String = { i, b -> "$i $b"}

class Baz5 : Foo {
    constructor() : super(lambda())
}