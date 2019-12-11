// !DIAGNOSTICS: -UNUSED_EXPRESSION -UNUSED_PARAMETER -UNUSED_VARIABLE -NOTHING_TO_INLINE
public class Z {
    internal val privateProperty = 11;

    internal fun privateFun() {

    }
}

public inline fun test() {
    Z().privateProperty
    Z().privateFun()
}

internal inline fun testInternal() {
    Z().privateProperty
    Z().privateFun()
}


public class Z2 {
    private val privateProperty = 11;

    private fun privateFun() {

    }

    public inline fun test() {
        privateProperty
        privateFun()
    }

    internal inline fun testInternal() {
        privateProperty
        privateFun()
    }
}