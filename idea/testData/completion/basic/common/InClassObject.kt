class C {
    val field = 0
    class NestedClass
    inner class InnerClass
    object AnObject

    default object {
        val classObjectField = 0
        class ClassObjectClass

        fun foo(){
            <caret>
        }
    }
}

fun C.extensionForC(){}

// ABSENT: field
// EXIST: NestedClass
// EXIST: InnerClass
// EXIST: AnObject
// EXIST: classObjectField
// EXIST: ClassObjectClass
// ABSENT: extensionForC
