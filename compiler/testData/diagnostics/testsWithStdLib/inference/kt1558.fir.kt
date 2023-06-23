// !CHECK_TYPE

//KT-1558 Exception while analyzing
package j

import checkSubtype

fun <T : Any> T?.sure() : T = this!!

fun <E> List<*>.toArray(ar: Array<E>): Array<E> = ar

fun testArrays(ci: List<Int?>, cii: List<Int?>?) {
    val c1: Array<Int?> = cii.sure().toArray(<!ARGUMENT_TYPE_MISMATCH, NO_COMPANION_OBJECT!>Array<Int?><!>)

    val c2: Array<Int?> = ci.toArray(Array<Int?><!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>)

    val c3 = Array<Int?><!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>

    val c4 = ci.toArray<Int?>(Array<Int?><!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>)

    val c5 = ci.toArray(Array<Int?><!NO_VALUE_FOR_PARAMETER, NO_VALUE_FOR_PARAMETER!>()<!>)

    checkSubtype<Array<Int?>>(c1)
    checkSubtype<Array<Int?>>(c2)
    checkSubtype<Array<Int?>>(c3)
    checkSubtype<Array<Int?>>(c4)
    checkSubtype<Array<Int?>>(c5)
}
