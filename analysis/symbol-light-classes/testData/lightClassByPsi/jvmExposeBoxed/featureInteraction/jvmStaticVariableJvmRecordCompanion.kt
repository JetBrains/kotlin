// WITH_STDLIB
// JVM_TARGET: 17
// LIBRARY_PLATFORMS: JVM

@JvmInline
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@JvmRecord
data class Rec(val i: Int) {
    companion object {
        @JvmStatic
        var baz: StringWrapper
            @JvmExposeBoxed
            get() = StringWrapper("OK")
            @JvmExposeBoxed
            set(value) {

            }
    }
}

// LIGHT_ELEMENTS_NO_DECLARATION: Rec.class[getBaz-K4fyztM;getBaz-K4fyztM;setBaz-JELJCFg;setBaz-JELJCFg], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
