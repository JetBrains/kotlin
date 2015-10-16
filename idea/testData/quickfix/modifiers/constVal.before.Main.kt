// "Add 'const' modifier" "true"
// ERROR: Only 'const val' can be used in constant expressions

package constVal

val i = 1

annotation class Fancy(val param: Int)

@Fancy(<caret>i) class D
