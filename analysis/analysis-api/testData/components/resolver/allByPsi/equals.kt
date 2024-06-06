class MyClass {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

open class Another

class Child : Another() {
    override fun equals(other: Any?): Boolean {
        return super.equals(other)
    }
}

fun myClass(m1: MyClass, m2: MyClass) {
    m1 === m2
    m1 !== m2
    m1 == m2
    m1 != m2
    m1.equals(m2)

    m2 === m1
    m2 !== m1
    m2 == m1
    m2 != m1
    m2.equals(m1)
}

fun another() {
    val another = Another()
    another === another
    another !== another
    another == Another()
    another != Another()
    another.equals(Another())

    val child = Child()
    val myClass = MyClass()
    child === another
    another !== child

    child == myClass
    child.equals(myClass)
    myClass != child
    !myClass.equals(child)

    another == child
    another.equals(child)
    child == another
    child.equals(another)

    another == myClass
    another.equals(myClass)
    myClass != another
    !myClass.equals(another)
}

// IGNORE_STABILITY_K1: candidates