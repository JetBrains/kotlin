import kotlin.platform.platformStatic
class A {
    <!WRONG_ANNOTATION_TARGET!>platformStatic<!> constructor() {}
    inner class B {
        <!WRONG_ANNOTATION_TARGET!>platformStatic<!> constructor() {}
    }
}

class C <!WRONG_ANNOTATION_TARGET!>platformStatic<!> constructor()