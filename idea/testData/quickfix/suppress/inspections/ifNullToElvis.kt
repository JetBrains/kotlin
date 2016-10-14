// "Suppress 'FoldInitializerAndIfToElvis' for fun foo" "true"

fun foo(p: List<String?>, b: Boolean) {
    var v = p[0]
    <caret>if (v == null) return
    if (b) v = null
}

// TOOL: org.jetbrains.kotlin.idea.intentions.FoldInitializerAndIfToElvisInspection