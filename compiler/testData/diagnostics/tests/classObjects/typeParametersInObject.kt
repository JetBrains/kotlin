object A<!TYPE_PARAMETERS_IN_OBJECT!><T><!>
object B<!TYPE_PARAMETERS_IN_OBJECT!><in T, out R><!>
object C<!TYPE_PARAMETERS_IN_OBJECT!><T : Comparable<T>><!>

class D {
    companion object<!TYPE_PARAMETERS_IN_OBJECT!><T><!>
}

class E {
    companion object<!TYPE_PARAMETERS_IN_OBJECT!><in T, out R><!>
}

class F {
    companion object C<!TYPE_PARAMETERS_IN_OBJECT!><T : Comparable<T>><!>
}

class G {
    companion object F<!TYPE_PARAMETERS_IN_OBJECT!><T><!>
}

object H<!TYPE_PARAMETERS_IN_OBJECT!><T, R><!><!CONSTRUCTOR_IN_OBJECT!>()<!>
