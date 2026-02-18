// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters +ImplicitJvmExposeBoxed

@JvmInline
value class A(val value: String)

@JvmInline
value class Z(val value: String)

context(_: Z)
var A.f: String
    get() = ""
    set(value) {

    }

// LIGHT_ELEMENTS_NO_DECLARATION: A.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], GlobalVariableContextAndReceiverKt.class[getF-0rlsLgg;setF-xxIERmE], Z.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]