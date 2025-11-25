// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
// LANGUAGE: +ImplicitJvmExposeBoxed

package one

@JvmInline
value class MyValueClass(val str: String)

interface BaseInterface {
    fun regularFunction() {}

    fun functionWithValueClassParameter(param: MyValueClass) {

    }

    val propertyWithValueClassParameter: MyValueClass? get() = null
}

// DECLARATIONS_NO_LIGHT_ELEMENTS: BaseInterface.class[functionWithValueClassParameter;propertyWithValueClassParameter]
// LIGHT_ELEMENTS_NO_DECLARATION: BaseInterface.class[functionWithValueClassParameter-rdfNfmQ;getPropertyWithValueClassParameter-BXGQg7w], MyValueClass.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]