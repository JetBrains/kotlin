// FILE: a/k.kt
package a

open class k {
    fun getK(): k? = null
    fun getI(): i? = null
    inner class i
}

// FILE: a/y.java
package a;

public class y {
    public k getK() { return null; }
}

// FILE: a/x.java
package a;

public class x extends k {
    public i getIFromJava() { return null; }
}

// FILE: test.kt
package a

fun test() = x().getK()
fun test2() = x().getI()
fun test3() = x().getIFromJava()
fun test4() = y().getK()