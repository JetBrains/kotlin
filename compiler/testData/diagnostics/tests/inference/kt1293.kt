//KT-1293 Kompiler doesn't show error when element of Array<Int?> is assigned to Int

package kt1293

fun main(args : Array<String>) {
    val intArray = arrayOfNulls<Int>(10)
    val <!UNUSED_VARIABLE!>i<!> : Int = <!TYPE_MISMATCH!>intArray[0]<!>
    requiresInt(<!TYPE_MISMATCH!>intArray[0]<!>)
}

fun requiresInt(<!UNUSED_PARAMETER!>i<!>: Int) {}
