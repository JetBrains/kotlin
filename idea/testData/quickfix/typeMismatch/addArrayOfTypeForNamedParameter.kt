// "Add doubleArrayOf wrapper" "true"

annotation class ArrAnn(val name: DoubleArray)

@ArrAnn(name = <caret>3.14) class My
