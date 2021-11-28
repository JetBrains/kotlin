// WITH_STDLIB

@file:[JvmName("Foo") JvmMultifileClass]
package test

// This test checks that we generate empty multi-file facade even if there is no delegates within it (only private functions in packages)
private fun privateOnly() {}
