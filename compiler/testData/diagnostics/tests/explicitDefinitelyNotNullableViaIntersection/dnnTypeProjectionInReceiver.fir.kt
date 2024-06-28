// ISSUE: KT-53792, KT-66369

class MyBoxInv<T : String?>(val value: T)

fun <T : String?> MyBoxInv<T & Any>.getSizeInv() = value.length

class MyBoxOut<out T : String?>(val value: T)

fun <T : String?> MyBoxOut<T & Any>.getSizeOut() = value.length

class MyBoxIn<in T : String?> {
    fun foo(arg: T) {
        arg.toString()
    }
}

fun <T : String?> MyBoxIn<T & Any>.fooExt(arg: T) {
    foo(<!ARGUMENT_TYPE_MISMATCH!>arg<!>)
}

fun main() {
    val emptyBoxInv = MyBoxInv<String?>(null)
    emptyBoxInv.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getSizeInv<!>()

    val emptyBoxOut = MyBoxOut<String?>(null)
    emptyBoxOut.<!CANNOT_INFER_PARAMETER_TYPE, UNRESOLVED_REFERENCE_WRONG_RECEIVER!>getSizeOut<!>()

    val emptyBoxIn = MyBoxIn<String?>()
    emptyBoxIn.fooExt(null)
}
