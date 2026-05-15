// FULL_JDK
// TARGET_BACKEND: JVM_IR

class A1 : java.util.ArrayList<String>()

class A2 : java.util.ArrayList<String>() {
    override fun remove(x: String): Boolean = true
}
