// A
// WITH_STDLIB
class A {
    @Synchronized
    @Strictfp
    fun f() {

    }

    @Transient
    @Volatile
    var c: String = ""
}

// FIR_COMPARISON
