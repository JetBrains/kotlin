// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

enum class Color { RED }

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class ColorBox(val color: Color)

@OptIn(ExperimentalStdlibApi::class)
@JvmInline
@JvmExposeBoxed
value class IntArrayBox(val array: IntArray)

@OptIn(ExperimentalStdlibApi::class)
@JvmExposeBoxed
fun nameOf(box: ColorBox): String = box.color.name

// LIGHT_ELEMENTS_NO_DECLARATION: Color.class[getEntries;valueOf;values], ColorBox.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], IntArrayBox.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], UnderlyingEnumAndArrayKt.class[nameOf-1H25SDg]
