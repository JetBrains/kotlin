// WITH_RUNTIME

abstract class A {
    @Deprecated("")
    abstract var warn: UByte

    @Deprecated("", level = DeprecationLevel.HIDDEN)
    abstract var hidden: UByte
}
