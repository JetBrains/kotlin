// MEMBER_CLASS_FILTER: org.jetbrains.kotlin.fir.symbols.impl.FirFieldSymbol
package one

class Boo

interface Foo {
    fun foo(b: Boo)
}

class Usa<caret>ge(prop: Foo) : Foo by prop {
    fun baz(s: String) {

    }
}