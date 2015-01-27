// FILE: File1.kt
package java.lang

private class SomeClass

// FILE: File2.kt
package pack

public open class SomeClass

// FILE: Main.kt
package a

import pack.*

class X : SomeClass()
