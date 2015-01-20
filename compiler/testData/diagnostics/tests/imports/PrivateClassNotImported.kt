// FILE: File1.kt
package pack1

public class SomeClass {
    private class N
    public open class PublicNested
}

// FILE: File2.kt
package pack2

public open class N

// FILE: Main.kt
package a

import pack1.SomeClass.*
import pack2.*

class X : N()
class Y : PublicNested()
