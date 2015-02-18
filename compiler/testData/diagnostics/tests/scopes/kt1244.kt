//KT-1244 Frontend allows access to private members of other classes

package kt1244

class A {
    private var a = ""
}

class B() {
    init {
        A().<!INVISIBLE_MEMBER!>a<!> = "Hello"
    }
}