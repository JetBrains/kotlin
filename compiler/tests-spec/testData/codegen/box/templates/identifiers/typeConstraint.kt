<!DIRECTIVES("HELPERS: REFLECT")!>

class A <<!ELEMENT(1)!>, <!ELEMENT(2)!>>
        where <!ELEMENT(1)!> : CharSequence,
              <!ELEMENT(2)!> : Comparable<<!ELEMENT(1)!>> {
    fun getValue() = false
}

annotation class B <<!ELEMENT(3)!>>
        where <!ELEMENT(3)!> : CharSequence,
              @A<List<Nothing?>> <!ELEMENT(3)!> : Comparable<<!ELEMENT(3)!>> {

}

annotation class C <<!ELEMENT(4)!>, <!ELEMENT(5)!>> where @property:C <!ELEMENT(4)!> : CharSequence, <!ELEMENT(5)!> : Comparable<<!ELEMENT(5)!>> {

}

fun <<!ELEMENT(4)!>, <!ELEMENT(5)!>> d(): Boolean
        where <!ELEMENT(4)!> : Any,
              <!ELEMENT(4)!> : Iterable<*>,
              <!ELEMENT(4)!> : Collection<*>,
              <!ELEMENT(4)!> : MutableCollection<*>,
              <!ELEMENT(5)!> : Comparable<<!ELEMENT(4)!>> = true

fun box(): String? {
    if (!d<MutableSet<Any>, Comparable<MutableSet<Any>>>()) return null
    if (A<String, String>().getValue()) return null

    if (!checkClassTypeParametersWithUpperBounds(
            A::class,
            listOf(
                Pair("<!ELEMENT_VALIDATION(1)!>", listOf("kotlin.CharSequence")),
                Pair("<!ELEMENT_VALIDATION(2)!>", listOf("kotlin.Comparable<<!ELEMENT(1)!>>"))
            )
        )) return null

    if (!checkClassTypeParametersWithUpperBounds(
            B::class,
            listOf(
                Pair("<!ELEMENT_VALIDATION(3)!>", listOf("kotlin.CharSequence", "kotlin.Comparable<<!ELEMENT(3)!>>"))
            )
        )) return null

    if (!checkClassTypeParametersWithUpperBounds(
            C::class,
            listOf(
                Pair("<!ELEMENT_VALIDATION(4)!>", listOf("kotlin.CharSequence")),
                Pair("<!ELEMENT_VALIDATION(5)!>", listOf("kotlin.Comparable<<!ELEMENT(5)!>>"))
            )
        )) return null

    return "OK"
}
