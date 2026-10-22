// KT-74654
// KT-90199
@Target(AnnotationTarget.TYPE)
annotation class Ann

class C {
    val (a: @An<caret>n Int, b) = 1 to 2
}
