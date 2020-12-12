// !CHECK_TYPE

//KT-1558 Exception while analyzing
package j

import checkSubtype

fun <T : Any> T?.sure() : T = this!!

fun <E> List<*>.toArray(ar: Array<E>): Array<E> = ar

fun testArrays(ci: List<Int?>, cii: List<Int?>?) {
    val c1: Array<Int?> = cii.sure().<!INAPPLICABLE_CANDIDATE!>toArray<!>(Array<Int?>)

    val c2: Array<Int?> = ci.toArray(<!INAPPLICABLE_CANDIDATE!>Array<!><Int?>())

    val c3 = <!INAPPLICABLE_CANDIDATE!>Array<!><Int?>()

    val c4 = ci.toArray<Int?>(<!INAPPLICABLE_CANDIDATE!>Array<!><Int?>())

    val c5 = ci.toArray(<!INAPPLICABLE_CANDIDATE!>Array<!><Int?>())

    checkSubtype<Array<Int?>>(c1)
    checkSubtype<Array<Int?>>(c2)
    checkSubtype<Array<Int?>>(c3)
    checkSubtype<Array<Int?>>(c4)
    checkSubtype<Array<Int?>>(c5)
}
