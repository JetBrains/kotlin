// pack.RegularClass
// LANGUAGE: +FullValueClasses
// WITH_STDLIB
package pack

sealed value class SealedValueClass(parameter: Int) {
    abstract val code: Int

    open fun describe(): String = "Sealed"
}

class RegularClass(override val code: Int, var mutable: String) : SealedValueClass(code) {
    override fun describe(): String = "Regular"
}
