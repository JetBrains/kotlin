// FILE: main.kt
package test

@MyAnno
class MyClass

// FILE: MyAnnotation.kt
package test

annotation class MyAnnotation

// FILE: alias1.kt
package test

typealias MyAnno = MyAnnotation

// FILE: alias2.kt
package test

// An erroneous redeclaration of the alias above; the resolver has to survive it
typealias MyAnno = MyAnnotation
