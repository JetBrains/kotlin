interface In<in T>
interface Out<out T>
interface Inv<T>

fun <T> getT(): T = null!!

class Test<in I, out O, P>(
        val type1: I,
        val type2: O,
        val type3: P,
        val type4: In<I>,
        val type5: In<O>,

        var type6: I,
        var type7: O,
        var type8: P,
        var type9: In<I>,
        var type0: In<O>,

        type11: I,
        type12: O,
        type13: P,
        type14: In<I>,
        type15: In<O>
)