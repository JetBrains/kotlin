// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
@file:OptIn(ExperimentalStdlibApi::class)

// A constructor cannot be renamed, so a regular and an exposed constructor differ only in their parameter types.
// A nullable value class leaves nothing to box, so a constructor which has only such value classes is generated once.

@JvmInline
value class IntWrapper(val i: Int)

@JvmInline
value class StringWrapper(val s: String?)

@JvmExposeBoxed
class AllNullable(val a: StringWrapper?, val b: IntWrapper?)

@JvmExposeBoxed
class SomeNotNull(val a: StringWrapper?, val b: IntWrapper)

@JvmExposeBoxed
class NoneNullable(val a: IntWrapper)

// LIGHT_ELEMENTS_NO_DECLARATION: AllNullable.class[getA-DSQDras;getB-qF8lFNU], IntWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], NoneNullable.class[getA-7j0DjTs], SomeNotNull.class[getA-DSQDras;getB-7j0DjTs], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
