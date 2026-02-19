// KT-69819
package test

class Foo<T> { fun foo() {} }

fun usage() {
    test.<expr>Foo</expr><String>::foo
}
