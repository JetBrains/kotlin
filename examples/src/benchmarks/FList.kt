namespace flist

abstract class FList<T> {
    abstract val head : T
    abstract val tail : FList<T>

    abstract fun plus(element: T) : FList<T>
}

class EmptyFList<T>() : FList<T> {
    override val head: T
        get() = throw java.util.NoSuchElementException()

    override val tail : EmptyFList<T>
        get() = EmptyFList<T>()

    override fun plus(element: T) : FList<T> = OneElementFList<T>(element)
}

class OneElementFList<T> (override val head: T): FList<T> {
    override val tail : EmptyFList<T>
        get() = EmptyFList<T>()

    override fun plus(element: T) : FList<T> = StandardFList<T>(element, this)
}

class StandardFList<T> (override val head: T, override val tail: FList<T>) : FList<T> {
    override fun plus(element: T) : FList<T> = StandardFList<T>(element, this)
}

fun <T> FList<T>.plus2(element: T): FList<T> =
    when(this) {
        is EmptyFList<*> => OneElementFList<T>(element)
        else => StandardFList<T>(element, this)
    }

fun <T> FList<T>.plus3(element: T) : FList<T> =
    if(this is EmptyFList<*>)
        OneElementFList<T>(element)
    else
        StandardFList<T>(element, this)

fun main(args: Array<String>) {
    for(k in 0..3) {
        val start0 = System.currentTimeMillis()

        var flist0 : FList<Int> = EmptyFList<Int>()
        for(i in 0..5000000)
        flist0 = flist0 + i

        System.out?.println(System.currentTimeMillis() - start0)

        val start = System.currentTimeMillis()

        var flist : FList<Int> = EmptyFList<Int>()
        for(i in 0..5000000)
        flist = flist.plus2(i)

        System.out?.println(System.currentTimeMillis() - start)

        val start2 = System.currentTimeMillis()

        var flist2 : FList<Int> = EmptyFList<Int>()
        for(i in 0..5000000)
        flist2 = flist2.plus3(i)

        System.out?.println(System.currentTimeMillis() - start2)
        System.out?.println()
    }
}
