interface In<in T>
interface Out<out T>
interface Inv<T>

fun <T> getT(): T = null!!

class Test<in I, out O, P>(
        val type1: <!TYPE_VARIANCE_CONFLICT("I", "in", "out", "I")!>I<!>,
        val type2: O,
        val type3: P,
        val type4: In<I>,
        val type5: In<<!TYPE_VARIANCE_CONFLICT("O", "out", "in", "In<O>")!>O<!>>,

        var type6: <!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "I")!>I<!>,
        var type7: <!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "O")!>O<!>,
        var type8: P,
        var type9: In<<!TYPE_VARIANCE_CONFLICT("I", "in", "invariant", "In<I>")!>I<!>>,
        var type0: In<<!TYPE_VARIANCE_CONFLICT("O", "out", "invariant", "In<O>")!>O<!>>,

        <!UNUSED_PARAMETER!>type11<!>: I,
        <!UNUSED_PARAMETER!>type12<!>: O,
        <!UNUSED_PARAMETER!>type13<!>: P,
        <!UNUSED_PARAMETER!>type14<!>: In<I>,
        <!UNUSED_PARAMETER!>type15<!>: In<O>
)