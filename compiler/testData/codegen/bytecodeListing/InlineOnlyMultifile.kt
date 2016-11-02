// WITH_RUNTIME

@file:[JvmName("Foo") JvmMultifileClass]
package test

fun foo() {
    inlineOnly<String>()
}

// No method should be generated in multifile facade for 'inlineOnly'
// Because 'inlineOnly' is private in file part (because it's inline-only) and can't be delegated from facade
public inline fun <reified T> inlineOnly() {}
