// FILE: J.java

import java.io.Serializable;

public class J {
    public <T extends Cloneable & Serializable> J(T t) {}
}

// FILE: K.kt

import java.io.Serializable

// TODO: report TYPE_MISMATCH here as well
fun cloneable(c: Cloneable) = J(c)

fun serializable(s: Serializable) = J(<!TYPE_MISMATCH!>s<!>)

fun <T> both(t: T) where T : Cloneable, T : Serializable = J(t)
