// FIR_COMPARISON
class C {
    val field = 0
    class NestedClass
    inner class InnerClass

    inner class N {
        fun foo(){
            <caret>
        }
    }
}

fun C.extensionForC(){}

// EXIST: field
// EXIST: NestedClass
// EXIST: InnerClass
// EXIST: extensionForC
