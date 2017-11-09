// WITH_RUNTIME

open class A<T1 : Any, T2, T3 : List<T1>> where T2 : Runnable, T2 : Cloneable, T3 : Runnable

val v: A<X, <caret>>

//Text: (T1 : Any, <highlight>T2</highlight>, T3 where T2 : Runnable, T2 : Cloneable, T3 : List<T1>, T3 : Runnable), Disabled: false, Strikeout: false, Green: false
