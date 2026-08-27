// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// JVM_EXPOSE_BOXED

package one

@JvmInline
value class MyValueClass(val str: String)

@JvmInline
value class NullableUnderlyingValueClass(val str: String?)

data class MyDataClass(val value: MyValueClass)

data class NullableDataClass(val value: MyValueClass?)

data class NullableUnderlyingDataClass(val value: NullableUnderlyingValueClass?)

// LIGHT_ELEMENTS_NO_DECLARATION: MyDataClass.class[component1-KOFEOT0;copy-rdfNfmQ;getValue-KOFEOT0], MyValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], NullableDataClass.class[component1-BXGQg7w;copy-jQZIiqc;getValue-BXGQg7w], NullableUnderlyingDataClass.class[component1-qDMLDz8;copy-kpLg2QU;getValue-qDMLDz8], NullableUnderlyingValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]