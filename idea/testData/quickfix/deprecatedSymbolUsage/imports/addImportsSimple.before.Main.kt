// "Replace with 'gau()'" "true"

package test

@Deprecated("...", ReplaceWith("gau()", "test.dependency.gau"))
fun gav() {
    test.dependency.gau()
}

fun use() {
    <caret>gav()
}