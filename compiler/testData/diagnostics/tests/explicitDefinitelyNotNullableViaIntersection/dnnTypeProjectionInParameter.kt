// ISSUE: KT-53792, KT-66369

class MyBoxInv<T : String?>(val value: T)

fun <T : String?> getSizeInv(box: MyBoxInv<T & Any>) = box.value.length

class MyBoxOut<out T : String?>(val value: T)

fun <T : String?> getSizeOut(box: MyBoxOut<T & Any>) = box.value.length

class MyBoxIn<in T : String?> {
    fun foo(arg: T) {
        arg.toString()
    }
}

fun <T : String?> foo(box: MyBoxIn<T & Any>, arg: T) {
    box.foo(<!TYPE_MISMATCH!>arg<!>)
}

fun main() {
    val emptyBoxInv = MyBoxInv<String?>(null)
    getSizeInv(<!TYPE_MISMATCH!>emptyBoxInv<!>)

    val emptyBoxOut = MyBoxOut<String?>(null)
    getSizeOut(<!TYPE_MISMATCH!>emptyBoxOut<!>)

    val emptyBoxIn = MyBoxIn<String?>()
    foo(emptyBoxIn, null)
}
