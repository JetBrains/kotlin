// WITH_RUNTIME

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    inlineReified<String>()
}

public inline fun <reified T> inlineReified() {}
