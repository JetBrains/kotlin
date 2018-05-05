// "Add annotation target" "true"
// ERROR: This annotation is not applicable to target 'expression'

package test

annotation class AnnTarget

fun println(v: Int) {}

fun apply() {
    var v = 0
    <caret>@AnnTarget v++
    println(v)
}