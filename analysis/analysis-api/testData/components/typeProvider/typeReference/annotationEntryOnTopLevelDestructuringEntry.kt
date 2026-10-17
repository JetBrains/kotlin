// KT-74654
// KT-90199
annotation class Ann

val (a, @An<caret>n b) = 1 to 2
