// FILE: main.kt
package test

@FirstAlias
class MyClass

// FILE: aliases.kt
package test

typealias FirstAlias = SecondAlias

// An erroneous cyclic redeclaration next to the real one:
// resolution has to terminate and still reach the annotation
typealias SecondAlias = FirstAlias
typealias SecondAlias = MyAnnotation

// FILE: MyAnnotation.kt
package test

annotation class MyAnnotation
