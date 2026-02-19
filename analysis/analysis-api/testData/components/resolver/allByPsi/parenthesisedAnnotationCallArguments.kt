annotation class MyAnno(val value: String)
annotation class MyAnnoVararg(vararg val value: String)
annotation class MyAnnoKClass(val value: kotlin.reflect.KClass<*>)

@MyAnno((("A")))
fun foo1() {}

@MyAnno(value = (("A")))
fun foo2() {}

@MyAnno((((("A")) as String)))
fun foo3() {}

@MyAnnoVararg((("A")), "B", *((arrayOf("A", "B", "C"))))
fun foo4() {}

@MyAnnoVararg(value = ((arrayOf(("A"), "B"))))
fun foo5() {}

@MyAnnoKClass((((String::class))))
fun foo6() {}