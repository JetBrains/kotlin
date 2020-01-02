// !WITH_NEW_INFERENCE
interface Base1 {
    fun test() = "OK"
}

interface Base2 {
    fun test2() = "OK"
}


class Delegate1 : Base1

class Delegate2 : Base2


public abstract class MyClass : Base1, Base2 {
    override fun test(): String {
        return "Class"
    }

    override fun test2(): String {
        return "Class"
    }
}

class A : MyClass(), Base1 by Delegate1(), Base1 by Delegate2() {

}