// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class Id(val value: String)

typealias Alias = Id

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun throughAlias(id: Alias): String = id.value

// DECLARATIONS_NO_LIGHT_ELEMENTS: TypealiasToValueClassKt.class[Alias]
// LIGHT_ELEMENTS_NO_DECLARATION: Id.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], TypealiasToValueClassKt.class[throughAlias-tmnojjU]
