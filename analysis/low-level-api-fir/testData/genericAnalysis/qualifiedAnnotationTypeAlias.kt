// FILE: main.kt
package test

@MyAnno
class MyClass

// FILE: alias.kt
package test

typealias MyAnno = other.MyAnnotation

// FILE: MyAnnotation.kt
package other

annotation class MyAnnotation
