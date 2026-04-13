// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

@JvmInline
value class StringWrapper constructor(val s: String?)

class Test(val s: StringWrapper?) {
    fun ok(): String = s!!.s!!
}
// LIGHT_ELEMENTS_NO_DECLARATION: StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Test.class[getS-DSQDras]