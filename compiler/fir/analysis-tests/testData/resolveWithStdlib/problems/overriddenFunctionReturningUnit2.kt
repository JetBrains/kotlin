// FILE: JK.java

public class JK extends K {
    @Override
    public void test() {

    }
}

// FILE: K.kt

fun jk() = JK().test()

open class K {
    open fun test() = Unit
}
