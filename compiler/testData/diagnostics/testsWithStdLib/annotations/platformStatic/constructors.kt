import kotlin.jvm.jvmStatic
class A {
    <!WRONG_ANNOTATION_TARGET!>jvmStatic<!> constructor() {}
    inner class B {
        <!WRONG_ANNOTATION_TARGET!>jvmStatic<!> constructor() {}
    }
}

class C <!WRONG_ANNOTATION_TARGET!>jvmStatic<!> constructor()