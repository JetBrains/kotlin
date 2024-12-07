// RUN_PIPELINE_TILL: BACKEND
// DUMP_CFG
interface B

interface A {
    val b: B?
}

class C(a: A, b: B) {
    init {
        val c = a.b?.let {
            C(a, it)
        }
    }
}
