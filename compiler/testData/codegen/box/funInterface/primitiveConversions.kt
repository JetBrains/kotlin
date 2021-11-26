// This test should check argument coercion between the SAM and the lambda.
// For now it checks that Char is boxed in JS

fun interface CharToAny {
    fun invoke(c: Char): Any
}

fun interface GenericToAny<T> {
    fun invoke(t: T): Any
}

fun interface GenericCharToAny: GenericToAny<Char>

fun foo1(c: CharToAny): Any = c.invoke('1')

fun <T> foo2(t: T, g: GenericToAny<T>): Any = g.invoke(t)

fun foo3(c: GenericCharToAny) = c.invoke('3')

fun box(): String {

    val c1 = foo1 { it }
    if (c1 !is Char || c1 != '1') return "fail1"

    val c2 = foo2<Char>('2') { it }
    if (c2 !is Char || c2 != '2') return "fail2"

    val c3 = foo3 { it }
    if (c3 !is Char || c3 != '3') return "fail3"

    val c4 = CharToAny { it }.invoke('4')
    if (c4 !is Char || c4 != '4') return "fail4"

    val c5 = GenericToAny<Char> { it }.invoke('5')
    if (c5 !is Char || c5 != '5') return "fail5"

    val c6 = GenericCharToAny { it }.invoke('6')
    if (c6 !is Char || c6 != '6') return "fail6"

    return "OK"
}
