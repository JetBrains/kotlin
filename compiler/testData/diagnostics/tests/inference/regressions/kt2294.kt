//KT-2294 Type inference infers DONT_CARE instead of correct type
package a

public fun foo<E>(array: Array<E>): Array<E> = array

public fun test()
{
    val x = foo(array(1, 2, 3, 4, 5)) // Should infer type 'Int'
    //            ^--- public final fun <T : jet.Any? > array(vararg t : DONT_CARE) : jet.Array<DONT_CARE> defined in Kotlin
    //       ^--- public final fun <E : jet.Any? > foo(items t : jet.Array<DONT_CARE>) : jet.Array<DONT_CARE> defined in root package
    x : Array<Int>
}

//--------------------

fun <T> array(vararg t : T) : Array<T> = t as Array<T>
