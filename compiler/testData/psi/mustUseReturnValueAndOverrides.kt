// WITH_STDLIB
// COMPILER_ARGUMENTS: -Xreturn-value-checker=check
// FILE: Base.kt

interface Base1 {
    fun unspecified(): String
}

@MustUseReturnValues
interface Base2 {
    fun mustUse(): String
    @IgnorableReturnValue fun ignorable(): String
}

// FILE: Impl.kt
class Impl1: Base1 {
    override fun unspecified(): String = ""
}

class Impl2: Base2 {
    override fun mustUse(): String = ""
    override fun ignorable(): String = ""
}

@MustUseReturnValues
class Impl3: Base2 {
    override fun mustUse(): String = ""
    @IgnorableReturnValue override fun ignorable(): String = ""
}
