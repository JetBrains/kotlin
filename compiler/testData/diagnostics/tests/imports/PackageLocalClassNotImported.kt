// FILE: File1.kt
package pack1

private class SomeClass

// FILE: File2.kt
package pack2

public open class SomeClass

// FILE: Main.kt
package a

import pack1.*
import pack2.*

class X : SomeClass()
