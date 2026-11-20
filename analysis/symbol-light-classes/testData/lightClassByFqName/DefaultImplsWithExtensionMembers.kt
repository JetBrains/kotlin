// A
// JVM_DEFAULT_MODE: disable

interface A {
    fun String.extension(x: Long) {}

    var String.extensionProperty: Int
        get() = 1
        set(value) {}

    context(c: Double)
    fun withContextParameter(x: Long) {}

    context(c: Double)
    fun Char.extensionWithContextParameter(x: Long) {}

    context(c: Double)
    val Char.extensionPropertyWithContextParameter: Int
        get() = 1
}
