// MODULE: dependency
// FILE: MySealedInterface.kt
sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

// MODULE: main(dependency)
// FILE: main.kt

// class: MySealedInterface
