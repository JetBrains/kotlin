// MODULE: dependency
// FILE: MySealedClass.kt
sealed class MySealedClass

typealias T1 = MySealedClass
typealias T2 = T1

class OneSealedChild : MySealedClass()
class TwoSealedChild : T1()
class ThreeSealedChild : T2()

// MODULE: main(dependency)
// FILE: main.kt

// class: MySealedClass
