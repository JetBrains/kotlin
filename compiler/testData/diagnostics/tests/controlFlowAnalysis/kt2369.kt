//KT-2369 Variable is not marked as uninitialized in 'finally' section

fun main(args: Array<String>) {
    var x : Int
    try {
        throw Exception()
    }
    finally {
        doSmth(<!UNINITIALIZED_VARIABLE!>x<!> + 1)
    }
}

fun doSmth(a: Any?) = a