// WITH_STDLIB
// !INHERIT_MULTIFILE_PARTS
// FILE: test.kt

@file:JvmMultifileClass
@file:JvmName("A")

private fun private(x: String = "") {}

private inline fun privateInline(x: String, y: Int = 0) {}

internal fun internal(x: String = "") {}

internal inline fun internalInline(x: String, y: Int = 0) {}

@PublishedApi
internal fun published(x: String = "") {}

@PublishedApi
internal fun publishedInline(x: String = "") {}

public fun public(x: String = "") {}

public inline fun publicInline(x: String, y: Int = 0) {}
