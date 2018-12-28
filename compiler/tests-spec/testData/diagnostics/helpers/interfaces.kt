interface EmptyInterface

interface Interface1 {
    fun itest() {}
    fun itest1() {}
}

interface Interface2 {
    fun itest() {}
    fun itest2() {}
}

interface Interface3 {
    fun itest() {}
    fun itest3() {}
}

interface InterfaceWithOutParameter<out T>

interface InterfaceWithTypeParameter1<T> {
    fun ip1test1(): T? = null as T?
}

interface InterfaceWithTypeParameter2<T> {
    fun ip1test2(): T? = null as T?
}

interface InterfaceWithTypeParameter3<T> {
    fun ip1test3(): T? = null as T?
}

interface InterfaceWithFiveTypeParameters1<T1, T2, T3, T4, T5> {
    fun itest() {}
    fun itest1() {}
}

interface InterfaceWithFiveTypeParameters2<T1, T2, T3, T4, T5> {
    fun itest() {}
    fun itest2() {}
}

interface InterfaceWithFiveTypeParameters3<T1, T2, T3, T4, T5> {
    fun itest() {}
    fun itest3() {}
}

interface InterfaceWithTwoTypeParameters<T, K> {
    fun ip2test(): T? = null as T?
}
