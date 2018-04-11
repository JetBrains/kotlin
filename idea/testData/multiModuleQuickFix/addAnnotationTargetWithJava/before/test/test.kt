package test

annotation class AnnTarget

fun apply() {
    var v = 0
    @AnnTarget<caret> v++
    println(v)
}