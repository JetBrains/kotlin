// "Replace with 'gau()'" "true"

package test

@Deprecated("...", ReplaceWith("gau()", "test.dependency.gau"))
fun gau() {
    test.dependency.gau()
}

fun use() {
    <caret>gau()
}