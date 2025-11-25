// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed
@JvmInline
value class Z(val value: String)

class A {
    var Z.f: String
        get() = ""
        set(value) {

        }
}

// LIGHT_ELEMENTS_NO_DECLARATION: A.class[getF-IQRRRT4;setF-QiIUSjo], Z.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]