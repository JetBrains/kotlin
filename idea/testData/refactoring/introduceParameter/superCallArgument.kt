class A : B({ "${<selection>C.c</selection>}" })

class C() {
    companion object {
        val c = 23
    }
}

open class B(param: () -> String)