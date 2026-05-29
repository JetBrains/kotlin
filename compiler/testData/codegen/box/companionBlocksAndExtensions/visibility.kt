// LANGUAGE: +CompanionBlocksAndExtensions

class C {
    companion {
        private val privateProp = "private"
        internal val internalProp = "internal"
        val publicProp = "public"

        private fun privateFun() = "privateFun"
        internal fun internalFun() = "internalFun"
        fun publicFun() = "publicFun"
    }

    // Private members accessible from within the class body
    fun accessPrivate() = privateProp + privateFun()
}

fun box(): String {
    // Public members accessible from outside
    if (C.publicProp != "public") return "FAIL: publicProp=${C.publicProp}"
    if (C.publicFun() != "publicFun") return "FAIL: publicFun=${C.publicFun()}"

    // Internal members accessible from same module
    if (C.internalProp != "internal") return "FAIL: internalProp=${C.internalProp}"
    if (C.internalFun() != "internalFun") return "FAIL: internalFun=${C.internalFun()}"

    // Private members accessible from within the class
    val c = C()
    if (c.accessPrivate() != "privateprivateFun") return "FAIL: accessPrivate=${c.accessPrivate()}"

    return "OK"
}
