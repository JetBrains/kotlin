// WITH_STDLIB
// TARGET_PLATFORM: JVM

fun foo(node: kotlinx.dom.Node) {
    <caret>check(node.isElement)
}