interface A

interface B

class Clazz1 : A, B
class Clazz2 : A, B

fun <K> select(x: K, y: K): K

fun test() = select(Clazz1(), Clazz2())

fun <T> makeNull(x: T): T? = null

fun testNull() = makeNull(select(Clazz1(), Clazz2()))