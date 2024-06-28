// FILE: JavaClass.java
public class JavaClass {
    class NestedInJavaClass {
    }
}

// FILE: main.kt
class A {
    fun memberInA() {}
}

class B {
    fun memberInB() {}
}

fun withA(f: A.() -> Unit) {}

fun withB(f: B.() -> Unit) {}

fun withJavaClass(f: JavaClass.() -> Unit) {}

fun topLevel() = 1

class C: JavaClass() {
    fun <T> methodInC(param: String?) {
        val localVarB = 2
        fun localFunB() {}
        param?.let { lambdaArg ->
            val localVarA = 1
            withB {
                withA {
                    <expr>e</expr>
                }
            }
        }
    }

    class NestedInC
}