// !CHECK_TYPE

//KT-2294 Type inference infers DONT_CARE instead of correct type
package a

public fun <E> foo(array: Array<E>): Array<E> = array

public fun test()
{
    val x = foo(array(1, 2, 3, 4, 5)) // Should infer type 'Int'
    //            ^--- public final fun <T : kotlin.Any? > array(vararg t : DONT_CARE) : kotlin.Array<DONT_CARE> defined in Kotlin
    //       ^--- public final fun <E : kotlin.Any? > foo(items t : kotlin.Array<DONT_CARE>) : kotlin.Array<DONT_CARE> defined in root package
    checkSubtype<Array<Int>>(x)
}

//--------------------
@Suppress("UNCHECKED_CAST")
fun <T> array(vararg t : T) : Array<T> = t as Array<T>
