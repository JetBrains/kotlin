// !LANGUAGE: +InlineClasses

inline class Z(val x: Int) {
    constructor(x: Long) : this(x.toInt())
    internal constructor(x: Int, y: Int) : this(x + y)
    private constructor(x: Short) : this(x.toInt())

    fun publicFun() {}
    internal fun internalFun() {}
    private fun privateFun() {}

    fun String.publicExtensionFun() {}
    internal fun String.internalExtensionFun() {}
    private fun String.privateExtensionFun() {}
    
    val publicVal: Int get() = x
    internal val internalVal: Int get() = x
    private val privateVal: Int get() = x

    val String.publicExtensionVal: Int get() = x
    internal val String.internalExtensionVal: Int get() = x
    private val String.privateExtensionVal: Int get() = x

    var publicVar: Int
        get() = x
        set(v) {}
    internal var internalVar: Int
        get() = x
        set(v) {}
    private var privateVar: Int
        get() = x
        set(v) {}

    var String.publicExtensionVar: Int
        get() = x
        set(v) {}
    internal var String.internalExtensionVar: Int
        get() = x
        set(v) {}
    private var String.privateExtensionVar: Int
        get() = x
        set(v) {}
}