class Test1<<!GENERIC_THROWABLE_SUBCLASS!>T<!>, B> : Exception() {
     inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class Test2<!><<!GENERIC_THROWABLE_SUBCLASS!>S<!>> : Throwable()
     class Test3 : NullPointerException()
     object Test4 : Throwable() {}
}

class Test5<T, B> {
     inner <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class Test6<!> : Exception()

     fun foo() {
         <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class Test7<!> : Throwable()
     }
}

fun <Z> topLevelFun() {
    <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>class Test8<!> : Error()
    val obj = <!INNER_CLASS_OF_GENERIC_THROWABLE_SUBCLASS!>object<!> : Throwable() {}
}
