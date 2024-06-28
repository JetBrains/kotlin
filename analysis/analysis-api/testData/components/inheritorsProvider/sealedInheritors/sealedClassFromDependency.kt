// MODULE: dependency
// FILE: MySealedClass.kt
package foo.bar

sealed class MySealedClass

class OneSealedChild : MySealedClass()
class TwoSealedChild : MySealedClass()

// MODULE: main(dependency)
// FILE: main.kt

// class: foo/bar/MySealedClass
