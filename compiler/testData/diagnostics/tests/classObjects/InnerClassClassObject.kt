// RUN_PIPELINE_TILL: FRONTEND
// http://youtrack.jetbrains.net/issue/KT-449

class A {
    inner class B {
        companion <!NESTED_CLASS_NOT_ALLOWED("Companion object")!>object<!> { }
    }
}

class B {
    companion object {
        class B {
            companion object {
                class C {
                    companion object { }
                }
            }
        }
    }
}

class C {
    class D {
        companion object { }
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, inner, nestedClass, objectDeclaration */
