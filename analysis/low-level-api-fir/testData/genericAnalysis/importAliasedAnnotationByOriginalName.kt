// FILE: main.kt
package test

import other.MyAnnotation as Renamed

@MyAnnotation
class MyClass

// FILE: MyAnnotation.kt
package other

annotation class MyAnnotation
