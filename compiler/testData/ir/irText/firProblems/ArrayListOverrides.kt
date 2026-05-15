// FULL_JDK
// TARGET_BACKEND: JVM_IR
// IGNORE_BACKEND: JKLIB
// ^KT-86348 java.lang.AssertionError: Can't find built-in class kotlin.Cloneable

class A1 : java.util.ArrayList<String>()

class A2 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
}
