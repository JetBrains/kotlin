typealias Test1 = List<dynamic>

typealias Test2 = (dynamic) -> dynamic

typealias GenList<T> = List<T>
typealias TestGen1 = GenList<dynamic>
typealias TestGen2 = GenList<GenList<dynamic>>

fun useGen1(x: TestGen1) = x
fun useGen2(x: TestGen2) = x

fun testUseGen1(x: List<dynamic>) = useGen1(x)
fun testUseGen2(x: List<List<dynamic>>) = useGen2(x)
