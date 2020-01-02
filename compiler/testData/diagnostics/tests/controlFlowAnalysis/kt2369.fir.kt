//KT-2369 Variable is not marked as uninitialized in 'finally' section

fun main() {
    var x : Int
    try {
        throw Exception()
    }
    finally {
        doSmth(x + 1)
    }
}

fun doSmth(a: Any?) = a