// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// MODULE: m1-common
// FILE: common.kt

public interface Encoder {
    public fun foo(x: Int = 0): Int
}

public interface ContentEncoder : Encoder {}

public expect object DeflateEncoder : ContentEncoder {
    override fun foo(
        x: Int
    ): Int
}

public object Identity : Encoder {
    override fun foo(
        x: Int
    ): Int = x
}


// MODULE: m2-js()()(m1-common)
// FILE: js.kt

public actual object DeflateEncoder : ContentEncoder, Encoder by Identity {
}
