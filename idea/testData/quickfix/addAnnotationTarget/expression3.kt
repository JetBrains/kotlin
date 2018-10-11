// "Add annotation target" "true"
@Retention
annotation class Foo

fun test() {
    var v = 0
    <caret>@Foo v++
}