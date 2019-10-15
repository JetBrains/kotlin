fun varargsOnlyFun(vararg args: Int): List<Int> {
    val result = ArrayList<Int>()
    for (i in args) {
        result.add(i)
    }
    return result

}

fun varargsMixedFun(lst: MutableList<Int>, vararg args: Int) {
    for (i in args) {
        lst.add(i)
    }
}

fun <T> varargsGenericFun(vararg args: T): List<T> = listOf(*args)

class ClassWithPrimaryCtorVarargs(vararg val args: Double)

class ClassWithSecondaryCtorVarargs() {
    constructor(vararg args: String) : this() {
        for (el in args) {
            println(el)
        }
    }
}

class GenericClassWithPrimaryCtorVarargs<T>(vararg val args: T) {
}

class GenericClassWithSecondaryCtorVarargs<T>() {

    constructor(vararg args: T) : this() {
        for (el in args) {
            println(el)
        }
    }
}

fun box(): String {
    val lst: List<Int> = varargsOnlyFun(1, 2, 3)
    val mutLst = mutableListOf<Int>()
    varargsMixedFun(mutLst, 1, 2, 3, )

    for (i in 0 until lst.size) {
        if (lst[i] != mutLst[i]) {
            return "FAIL"
        }
    }

    val mutLstImplicitGen = varargsGenericFun<String>("foo", "bar", "baz")
    val mutLstExplicitGen = varargsGenericFun("foo", "bar", "baz")

    for (j in 0 until mutLstImplicitGen.size) {
        if (mutLstImplicitGen[j] !is String
            || mutLstExplicitGen[j] !is String
            || mutLstImplicitGen[j] != mutLstExplicitGen[j]
        ) {
            return "FAIL"
        }
    }

    val classWithPrimaryCtorVarargs = ClassWithPrimaryCtorVarargs(1.0, 2.0, 3.0)
    val genericClassWithPrimaryCtorVarargs = GenericClassWithPrimaryCtorVarargs(1.0, 2.0, 3.0)

    for (t in 0 until classWithPrimaryCtorVarargs.args.size) {
        if (classWithPrimaryCtorVarargs.args[t] != genericClassWithPrimaryCtorVarargs.args[t]) {
            return "FAIL"
        }
    }

    val classWithSecondaryCtorVarargs = ClassWithSecondaryCtorVarargs("foo", "bar", "baz")
    val genericClassWithSecondaryCtorVarargs = GenericClassWithSecondaryCtorVarargs("foo", "bar", "baz")
    return "OK"

}