// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Native:2.4

interface Interface

interface AstNode

class CompositeNode : AstNode

class LeafNode : AstNode

fun classify(node: AstNode): String {
  return node.takeIf { false }?.let {
    "fail: unreachable"
  } ?: node.takeIf { it is CompositeNode }?.let {
    "fail: composite"
  } ?: "OK"
}

fun box(): String {
    val node = LeafNode()
    return classify(node)
}
