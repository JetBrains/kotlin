open class MyClass() {
}

class A() : My<caret> {
    public fun test() {
        val a : MyC<caret>
    }
}

// EXIST: MyClass