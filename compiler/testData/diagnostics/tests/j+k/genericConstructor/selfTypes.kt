// !DIAGNOSTICS: -UNUSED_VARIABLE
// FILE: R.java
public class R<T extends R<T>> {
    public <F extends R<F>> R(F x) {
    }
}

// FILE: RImpl.java
public class RImpl extends R<RImpl> {
    public RImpl() {
        <RImpl>super(null);
    }
}

// FILE: main.kt
fun test() {
    val x: R<RImpl> = R(RImpl())
    R<RImpl, RImpl>(RImpl())
}
