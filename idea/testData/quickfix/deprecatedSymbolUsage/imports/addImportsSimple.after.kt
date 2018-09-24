// "Replace with 'gau()'" "true"

package test

import test.dependency.gau

@Deprecated("...", ReplaceWith("gau()", "test.dependency.gau"))
fun gav() {
    test.dependency.gau()
}

fun use() {
    gau()
}