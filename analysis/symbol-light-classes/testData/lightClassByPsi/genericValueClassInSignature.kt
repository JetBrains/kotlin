// WITH_STDLIB
// LIBRARY_PLATFORMS: JVM
package test

@JvmInline
value class AsList<T : Any>(val x: List<T>)

class Wrapper<T>(val v: T)

@JvmInline
value class WithWrapper<T : Any>(val x: Wrapper<T>)

@JvmInline
value class Arr<T : CharSequence>(val a: Array<T>)

class Holder(val ctorParam: AsList<List<Int>>) {
    val list: AsList<Int> = AsList(listOf(1))

    val nullable: WithWrapper<String>? = null

    val arr: Arr<String> = Arr(arrayOf("a"))

    @JvmName("takeList")
    fun takeList(x: AsList<List<Int>>) {}

    @JvmName("give")
    fun give(): WithWrapper<Int>? = null

    @JvmName("generic")
    fun <R : Any> generic(x: AsList<R>) {}
}

// LIGHT_ELEMENTS_NO_DECLARATION: Arr.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], AsList.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl], Holder.class[getArr-mZM3UDs;getCtorParam-gDuY8Gg;getList-gDuY8Gg;getNullable-9UDvz2U], WithWrapper.class[constructor-impl;equals-impl;equals-impl0;hashCode-impl;toString-impl]
