// "Add 'const' modifier" "true"
val i = 1

annotation class Fancy(val param: Int)

@Fancy(<caret>i) class D
