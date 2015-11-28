open class A(@JvmField public val publicField: String = "1",
             @JvmField internal val internalField: String = "2",
             @JvmField protected val protectedfield: String = "3")

open class B : A()