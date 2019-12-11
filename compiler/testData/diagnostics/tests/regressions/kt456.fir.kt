//KT-456 No check for obligatory return in getters

package kt456

class A() {
    val i: Int
    get() : Int {  //no error
    }
}

//more tests
class B() {
    val i: Int
    get() {  //no error
    }
}

class C() {
    val i : Int
    get() : Int {
        try {
            doSmth()
        }
        finally {
            doSmth()
        }
    }
}

fun doSmth() {}
