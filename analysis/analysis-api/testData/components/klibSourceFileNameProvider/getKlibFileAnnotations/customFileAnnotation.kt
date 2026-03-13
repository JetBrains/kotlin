// MODULE: library
// TARGET_PLATFORM: JS
// MODULE_KIND: LibraryBinary

// FILE: lib.kt
@file:MyFileAnnotation

package test

@Target(AnnotationTarget.FILE)
@Retention(AnnotationRetention.BINARY)
annotation class MyFileAnnotation

fun baz() = "baz"

// MODULE: main(library)
