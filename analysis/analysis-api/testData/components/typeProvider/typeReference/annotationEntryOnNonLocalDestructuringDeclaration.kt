// KT-74654
annotation class Ann

class C {
    @An<caret>n
    val (a, b) = 1 to 2
}
