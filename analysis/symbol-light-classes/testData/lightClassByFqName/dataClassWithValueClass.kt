// one.MyDataClass
// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM

package one

@JvmInline
value class MyValueClass(val str: String)

data class MyDataClass(val value: MyValueClass)
// DECLARATIONS_NO_LIGHT_ELEMENTS: MyDataClass.class[component1;copy]
// LIGHT_ELEMENTS_NO_DECLARATION: MyDataClass.class[component1-KOFEOT0;copy-rdfNfmQ;getValue-KOFEOT0]