// FILE: JK.java

public class JK extends K {
    @Override
    public void test() {

    }
}

// FILE: K.kt

open class K {
    open fun test() = Unit
}

fun jk() {
    JK().test()
}
