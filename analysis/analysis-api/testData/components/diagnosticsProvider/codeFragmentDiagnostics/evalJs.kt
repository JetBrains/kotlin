// WITH_STDLIB
// TARGET_PLATFORM: JS

// The test unfortunately fails, as stub loading from klibs (such as kotlin-stdilb-js.klib)
// in 'KotlinStaticDeclarationProvider' is not implemented.

fun foo(node: kotlinx.dom.Node) {
    <caret>check(node.isElement)
}