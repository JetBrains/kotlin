// FILE: A.kt
open class A {
    open fun getScope(): String? = null
    fun setScope(scope: String?): A {
        return this
    }

    protected var scope: String? = null
}

// FILE: B.java
public class B extends A {
    @java.lang.Override
    public String getScope() {
        return null;
    }
}

// FILE: main.kt
fun test(b: B) {
    val s = b.getScope()
    b.setScope(s)
}
