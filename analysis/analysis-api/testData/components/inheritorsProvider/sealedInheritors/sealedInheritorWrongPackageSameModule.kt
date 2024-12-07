// FILE: main.kt
package foo.bar

sealed class MySealedClass

class Inheritor1 : MySealedClass()

// FILE: Inheritor2.kt
package foo

import foo.bar.MySealedClass

class Inheritor2 : MySealedClass()

// class: foo/bar/MySealedClass
