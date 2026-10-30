// one.C
// LANGUAGE: +CompanionBlocks +CompanionExtensions
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package one

@JvmInline
value class C(val value: Int) {
    fun member(): Int = value

    companion {
        fun describe(): String = "describe"
        fun plain(v: Int): Int = v
        val label: String get() = "label"
        var counter: Int = 0
        internal fun internalFun(): Int = 1
        private fun privateFun(): Int = 2

        @JvmOverloads
        fun withDefault(a: Int, b: Int = 0): Int = a + b

        // The value class in the signature still mangles the name
        fun fromInt(value: Int): C = C(value)
        var current: C = C(0)
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: C.class[fromInt;member]
// LIGHT_ELEMENTS_NO_DECLARATION: C.class[constructor-impl;equals-impl;equals-impl0;fromInt-Aykwt80;getCurrent-r5-ypx8;hashCode-impl;member-impl;setCurrent-x_RLm0c;toString-impl]
