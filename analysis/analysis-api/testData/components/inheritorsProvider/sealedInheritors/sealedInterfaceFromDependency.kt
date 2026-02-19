// MODULE: dependency
// FILE: MySealedInterface.kt
package foo.bar

sealed interface MySealedInterface

class OneSealedChild : MySealedInterface
class TwoSealedChild : MySealedInterface

// MODULE: main(dependency)
// FILE: main.kt

// class: foo/bar/MySealedInterface
