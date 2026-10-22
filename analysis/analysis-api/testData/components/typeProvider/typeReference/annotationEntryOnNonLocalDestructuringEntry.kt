// KT-74654
// KT-90199
annotation class Ann

class C {
    val (@An<caret>n a, b) = 1 to 2
}
