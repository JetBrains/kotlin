// LANGUAGE: +NestedTypeAliases

interface I {
    <!TOPLEVEL_TYPEALIASES_ONLY!>typealias Foo = String<!>
}

val jsCode: I.Foo = js("console.log('Hello World')")
