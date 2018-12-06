interface In<in T>

inline fun <reified T> In<T>.isT(): Boolean =
    this is T

inline fun <reified T> In<T>.asT() { this as T }

fun <S> sel(x: S, y: S): S = x

interface A
interface B

interface A1 : A
interface A2 : A

interface Z1 : A, B
interface Z2 : A, B


fun testInIs1(x: In<A>, y: In<B>) = sel(x, y).isT()

fun testInIs2(x: In<Z1>, y: In<Z2>) = sel(x, y).isT()

fun testInIs3(x: In<A1>, y: In<A2>) = sel(x, y).isT()

fun testInAs1(x: In<A>, y: In<B>) = sel(x, y).asT()

fun testInAs2(x: In<Z1>, y: In<Z2>) = sel(x, y).asT()

fun testInAs3(x: In<A1>, y: In<A2>) = sel(x, y).asT()
