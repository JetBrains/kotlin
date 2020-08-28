// FIR_COMPARISON
package some

open class MyClass() {
}

class A() {
    public fun test() {
        val a : MyC<caret>
    }
}

// EXIST: MyClass
