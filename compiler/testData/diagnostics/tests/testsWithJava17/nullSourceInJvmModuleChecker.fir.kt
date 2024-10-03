// ISSUE: KT-71943
// WITH_STDLIB

import sun.awt.image.*

fun withCustomDecoders(originalGetDecoder: () -> <!JAVA_MODULE_DOES_NOT_EXPORT_PACKAGE!>ImageDecoder?<!>) {}
fun createImage() = withCustomDecoders { null }
