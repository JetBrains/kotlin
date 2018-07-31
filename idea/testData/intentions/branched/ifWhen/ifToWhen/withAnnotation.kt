var b = true

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun println(s: String) {}

fun foo() {
    <caret>if (@Ann b) {
        println("!")
    }
    else {
        println("")
    }
}