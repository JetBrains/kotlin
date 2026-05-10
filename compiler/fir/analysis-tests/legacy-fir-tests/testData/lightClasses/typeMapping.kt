interface A<T>
interface Pair<T1, T2>
interface Triple<T1, T2, T3>

class B1 : A<String>
class B2 : A<Pair<in String, out String>>
class B3 : A<A<*>>
class B4 : A<A<String>>
class B5 : A<List<A<*>>>
class B6 : A<Int>
class B7 : A<IntArray>
class B8 : A<Array<String>>
class B9 : A<Array<Int>>
class B10 : A<Array<IntArray>>

// LIGHT_CLASS_FQ_NAME: B1, B2, B3, B4, B5, B6, B7, B8, B9, B10
