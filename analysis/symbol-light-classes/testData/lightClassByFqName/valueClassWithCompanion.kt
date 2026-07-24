// pack.StringWrapper
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package pack

@JvmInline
value class StringWrapper(val s: String) {
    companion object {
        @JvmStatic
        fun unwrap(s: StringWrapper): String = s.s

        @JvmStatic
        fun regularStaticFunction() {}

        @JvmStatic
        var staticVariable: StringWrapper get() = StringWrapper("OK")
            set(value) {

            }

        @JvmStatic
        var regularStaticVariable: Int get() = 0
            set(value) {

            }
    }
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: StringWrapper.class[staticVariable;unwrap]
// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;getStaticVariable-48Mvbu4;getStaticVariable-48Mvbu4;hashCode-impl;setStaticVariable-m6LkbEo;setStaticVariable-m6LkbEo;toString-impl;unwrap-m6LkbEo;unwrap-m6LkbEo]
