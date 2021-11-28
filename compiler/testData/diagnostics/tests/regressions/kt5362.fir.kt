// KT-5362 Compiler crashes on access to extension method from nested class
class Outer {
    class Nested{
        fun foo(s: String) = s.<!UNRESOLVED_REFERENCE!>extension<!>()
    }

    private fun String.extension(): String = this
}

// EA-64302 - UOE: CodegenContext.getOuterExpression
fun Activity.toast() = Unit
class Activity(){
    class Fragment{
        fun call() = <!UNRESOLVED_REFERENCE_WRONG_RECEIVER!>toast<!>()
    }
}

// KT-8814 No error in IDE for invalid invoke of OuterClass.()->Unit in static nested class
public class Manager {
    fun task(callback: Manager.() -> Unit): Task {
        val task = Task(callback)
        return task
    }

    class Task(val callback: Manager.() -> Unit) : Runnable {
        override public fun run() {
            <!ARGUMENT_TYPE_MISMATCH!>callback<!>() // Manager is not accessible here, but no error is shown
        }
    }
}
