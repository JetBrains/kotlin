open class C1
open class C2
open class C3 : C2()

class A1<T> where T : C1, T : C2
class A2<T> where T : C1, T : C2, T : C3
class A3<T> where T : C2, T : C3
class A4<T> where T : C3, T : C2

fun <T> f1() where T : C1, T : C2, T : C3 {}
fun <T> f2() where T : C2, T : C3 {}
fun <T> f3() where T : C3, T : C2 {}

enum class E1
class A5<T> where T : C1, T : E1

object O1
class A6<T> where T : O1, T : C2
