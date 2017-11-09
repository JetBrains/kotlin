// WITH_RUNTIME

package test

typealias Global = String
fun usesGlobal(p: List<Global>) {
    p.map { <caret>it.toUpperCase() }
}