// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

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

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[Companion;constructor-impl;equals-impl;equals-impl0;getStaticVariable-K4fyztM;getStaticVariable-K4fyztM;hashCode-impl;setStaticVariable-JELJCFg;setStaticVariable-JELJCFg;toString-impl;unwrap-JELJCFg;unwrap-JELJCFg]