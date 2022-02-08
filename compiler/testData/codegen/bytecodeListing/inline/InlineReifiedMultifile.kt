// WITH_STDLIB

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    inlineReified<String>()
}

public inline fun <reified T> inlineReified() {}
