open class MySecondClass() {
}

open class MyFirstClass<T> {

}

class A() : My<caret> {
    public fun test() {
    }
}

// EXIST: MySecondClass, MyFirstClass
// FIR_COMPARISON
