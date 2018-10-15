class A <<!ELEMENT!>, <!ELEMENT!>>
        where <!ELEMENT!> : CharSequence,
              <!ELEMENT!> : Comparable<<!ELEMENT!>>

annotation class B <<!ELEMENT!>>
        where <!ELEMENT!> : CharSequence,
              @A<List<Nothing?>> @B <!ELEMENT!> : Comparable<<!ELEMENT!>>

annotation class C <<!ELEMENT!>, <!ELEMENT!>> where @property:C <!ELEMENT!> : CharSequence, <!ELEMENT!> : Comparable<<!ELEMENT!>>

fun <<!ELEMENT!>, <!ELEMENT!>> d(): Boolean
        where <!ELEMENT!> : Any,
              <!ELEMENT!> : Iterable<*>,
              <!ELEMENT!> : Collection<*>,
              <!ELEMENT!> : MutableCollection<*>,
              <!ELEMENT!> : Comparable<<!ELEMENT!>> = <!ELEMENT!> == <!ELEMENT!>