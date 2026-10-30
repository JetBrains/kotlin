// FILE: main.kt
package test

@MyAnno
@other.OtherAnno
class MyClass

// FILE: alias.kt
package test

typealias MyAnno = MyAnnotation

// FILE: MyAnnotation.kt
package test

annotation class MyAnnotation

// FILE: other.kt
package other

typealias OtherAnno = test.MyAnnotation
