class C {
    val field = 0
    class NestedClass
    inner class InnerClass

    class N {
        fun foo(){
            <caret>
        }
    }

    default object {
        fun fromClassObject(){}
    }
}

fun C.extensionForC(){}

// ABSENT: field
// EXIST: NestedClass
// EXIST: InnerClass
// EXIST: fromClassObject
// ABSENT: extensionForC
