import kotlin.jvm.JvmStatic
class A {
    <!WRONG_ANNOTATION_TARGET!>JvmStatic<!> constructor() {}
    inner class B {
        <!WRONG_ANNOTATION_TARGET!>JvmStatic<!> constructor() {}
    }
}

class C <!WRONG_ANNOTATION_TARGET!>JvmStatic<!> constructor()