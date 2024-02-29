// MODULE: dependency
// FILE: MySealedClass.kt
sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(dependency)
// FILE: main.kt

// class: MySealedClass
