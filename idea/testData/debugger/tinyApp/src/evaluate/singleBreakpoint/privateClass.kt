package privateClass

fun main(args: Array<String>) {
    //Breakpoint!
    args.size
}

class A {
    private class PrivateClass {
        private val prop = 1
    }

    private inner class PrivateInnerClass {
        private val prop = 1
    }
}

// EXPRESSION: A.PrivateClass()
// RESULT: instance of privateClass.A$PrivateClass(id=ID): LprivateClass/A$PrivateClass;

// EXPRESSION: A.PrivateClass().prop
// RESULT: 1: I

// EXPRESSION: A().PrivateInnerClass()
// RESULT: instance of privateClass.A$PrivateInnerClass(id=ID): LprivateClass/A$PrivateInnerClass;

// EXPRESSION: A().PrivateInnerClass().prop
// RESULT: 1: I

// EXPRESSION: forTests.MyJavaClass.PrivateJavaClass()
// RESULT: instance of forTests.MyJavaClass$PrivateJavaClass(id=ID): LforTests/MyJavaClass$PrivateJavaClass;

// EXPRESSION: forTests.MyJavaClass.PrivateJavaClass().prop
// RESULT: 1: I
