// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ContextParameters +ImplicitJvmExposeBoxed
@JvmInline
value class B(val value: String)

@JvmInline
value class Z(val value: String)

class A {
    context(_: Z, _: Boolean)
    var B.f: Int
        get() = 1
        set(value) {

        }
}
// LIGHT_ELEMENTS_NO_DECLARATION: A.class[getF-EVYOzKg;setF-vJ3jVlM], B.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Z.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]