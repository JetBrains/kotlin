fun varargsOnlyFun(vararg args: Int) : List<Int>  {
    val result : ArrayList<E> = ArrayList<Int>()
    val tmp0_iterator : IntIterator = args.iterator()
    while (tmp0_iterator.hasNext()) {
        val i : Int = tmp0_iterator.next()
        result.add(i)
    }
    return result
}

fun varargsMixedFun(lst: MutableList<Int>, vararg args: Int) {
    val tmp0_iterator : IntIterator = args.iterator()
    while (tmp0_iterator.hasNext()) {
        val i : Int = tmp0_iterator.next()
        lst.add(i)
    }
}

fun <T> varargsGenericFun(vararg args: T) : List<T>  {
    return     listOf(*args)
}

class ClassWithPrimaryCtorVarargs(vararg val args: Double) {
}
class ClassWithSecondaryCtorVarargs() {
    constructor(vararg args: String) : this() {
        val tmp0_iterator : Iterator<String> = args.iterator()
        while (tmp0_iterator.hasNext()) {
            val el : String = tmp0_iterator.next()
            println(el)
        }
    }
}
class GenericClassWithPrimaryCtorVarargs<T>(vararg val args: T) {
}
class GenericClassWithSecondaryCtorVarargs<T>() {
    constructor(vararg args: T) : this() {
        val tmp0_iterator : Iterator<T> = args.iterator()
        while (tmp0_iterator.hasNext()) {
            val el : T = tmp0_iterator.next()
            println(el)
        }
    }
}
fun box() : String  {
    val lst : List<Int> = varargsOnlyFun(1, 2, 3)
    val mutLst : MutableList<Int> = mutableListOf()
    varargsMixedFun(mutLst, 1, 2, 3)
    val tmp0_iterator : IntIterator = 0.until(lst.size).iterator()
    while (tmp0_iterator.hasNext()) {
        val i : Int = tmp0_iterator.next()
        if (lst.get(i) != mutLst.get(i)) {
            return "FAIL"
        }
    }
    val mutLstImplicitGen : List<String> = varargsGenericFun("foo", "bar", "baz")
    val mutLstExplicitGen : List<String> = varargsGenericFun("foo", "bar", "baz")
    val tmp1_iterator : IntIterator = 0.until(mutLstImplicitGen.size).iterator()
    while (tmp1_iterator.hasNext()) {
        val j : Int = tmp1_iterator.next()
        if (mutLstImplicitGen.get(j) !is String || mutLstExplicitGen.get(j) !is String || mutLstImplicitGen.get(j) != mutLstExplicitGen.get(j)) {
            return "FAIL"
        }
    }
    val classWithPrimaryCtorVarargs : ClassWithPrimaryCtorVarargs = ClassWithPrimaryCtorVarargs(1.0, 2.0, 3.0)
    val genericClassWithPrimaryCtorVarargs : GenericClassWithPrimaryCtorVarargs<Double> = GenericClassWithPrimaryCtorVarargs<Double>(1.0, 2.0, 3.0)
    val tmp2_iterator : IntIterator = 0.until(classWithPrimaryCtorVarargs.args.size).iterator()
    while (tmp2_iterator.hasNext()) {
        val t : Int = tmp2_iterator.next()
        if (classWithPrimaryCtorVarargs.args.get(t) != genericClassWithPrimaryCtorVarargs.args.get(t)) {
            return "FAIL"
        }
    }
    val classWithSecondaryCtorVarargs : ClassWithSecondaryCtorVarargs = ClassWithSecondaryCtorVarargs("foo", "bar", "baz")
    val genericClassWithSecondaryCtorVarargs : GenericClassWithSecondaryCtorVarargs<String> = GenericClassWithSecondaryCtorVarargs<String>("foo", "bar", "baz")
    return "OK"
}
