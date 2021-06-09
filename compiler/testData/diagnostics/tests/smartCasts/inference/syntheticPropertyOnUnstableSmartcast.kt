// FIR_IDENTICAL
// FILE: p/Super.java

package p

public interface Super {
    public String getName()
    public void setName(String name)
}

// FILE: p/test.kt

package p

class Sub : Super {
    val onlyInSub: Int = 1
    override fun getName(): String = ""
    override fun setName(name: String) {}
}

var s: Super = Sub()

fun test() {
    if (s is Sub) {
        s.name
        s.name = ""
        <!SMARTCAST_IMPOSSIBLE!>s<!>.onlyInSub
    }
}
