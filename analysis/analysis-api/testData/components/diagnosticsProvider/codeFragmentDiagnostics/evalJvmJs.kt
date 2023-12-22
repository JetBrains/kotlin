// WITH_STDLIB
// TARGET_PLATFORM: JVM, JS

fun foo(node: kotlinx.dom.Node) {
    <caret>check(node.isElement)
}