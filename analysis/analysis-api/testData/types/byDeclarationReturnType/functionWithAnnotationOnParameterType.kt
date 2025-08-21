@Target(AnnotationTarget.TYPE)
annotation class SomeAnno(val name: String)

fun foo(x : @SomeAnno("myFakeName") Int) {}

fun main() {
    val r<caret>ef = ::foo
}