// TARGET_BACKEND: JVM
// LANGUAGE: +CollectionLiterals
// WITH_REFLECT

import kotlin.test.assertEquals

annotation class Recursive(val arr: Array<Recursive>)

@Recursive([Recursive(arrayOf(Recursive([Recursive([]), Recursive([]), Recursive([])])))])
class Obj

fun box(): String {
    val recursive = Obj::class.annotations.single() as Recursive
    assertEquals(1, recursive.arr.size)
    assertEquals(1, recursive.arr[0].arr.size)
    assertEquals(3, recursive.arr[0].arr[0].arr.size)

    return "OK"
}
