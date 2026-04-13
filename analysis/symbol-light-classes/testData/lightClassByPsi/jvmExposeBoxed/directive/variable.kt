// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class StringWrapper(val s: String) {
    var ok: String get() = s
        set(value) {

        }
}

// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;getOk-impl;hashCode-impl;setOk-impl;toString-impl]