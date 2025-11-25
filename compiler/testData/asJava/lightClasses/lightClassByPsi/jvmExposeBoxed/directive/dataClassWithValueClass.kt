// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package one

@JvmInline
value class MyValueClass(val str: String)

data class MyDataClass(val value: MyValueClass)

// LIGHT_ELEMENTS_NO_DECLARATION: MyDataClass.class[MyDataClass;component1-KOFEOT0;copy-rdfNfmQ;getValue-KOFEOT0], MyValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]