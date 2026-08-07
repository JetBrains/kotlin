// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

// Unsigned types are valid annotation member types, so an annotation class really can carry a value class -
// unlike user value classes, which are INVALID_TYPE_OF_ANNOTATION_MEMBER. Annotation methods must keep their
// declared name and their erased return type, so they are never mangled and the class-level annotation must
// leave the light class exactly as it is.
@OptIn(ExperimentalStdlibApi::class, ExperimentalUnsignedTypes::class)
@JvmExposeBoxed
annotation class Unsigned(val count: UInt, vararg val more: UInt)

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class StringWrapper(val s: String)

@OptIn(ExperimentalStdlibApi::class)
@Unsigned(1u)
@JvmExposeBoxed
class Annotated(val wrapper: StringWrapper)

// LIGHT_ELEMENTS_NO_DECLARATION: Annotated.class[getWrapper-K4fyztM], StringWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
