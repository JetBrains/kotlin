// LANGUAGE: +NestedTypeAliases

interface I {
    typealias Foo = String
}

val jsCode: I.Foo = js("console.log('Hello World')")
