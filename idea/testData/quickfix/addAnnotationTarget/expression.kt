// "Add annotation target" "true"
@Retention(AnnotationRetention.SOURCE)
annotation class Foo

fun test() {
    var v = 0
    <caret>@Foo v++
}