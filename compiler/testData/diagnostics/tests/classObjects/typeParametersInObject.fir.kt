<!TYPE_PARAMETERS_IN_OBJECT!>object A<!><T>
<!TYPE_PARAMETERS_IN_OBJECT!>object B<!><in T, out R>
<!TYPE_PARAMETERS_IN_OBJECT!>object C<!><T : Comparable<T>>

class D {
    companion <!TYPE_PARAMETERS_IN_OBJECT!>object<!><T>
}

class E {
    companion <!TYPE_PARAMETERS_IN_OBJECT!>object<!><in T, out R>
}

class F {
    companion <!TYPE_PARAMETERS_IN_OBJECT!>object C<!><T : Comparable<T>>
}

class G {
    companion <!TYPE_PARAMETERS_IN_OBJECT!>object F<!><T>
}

<!TYPE_PARAMETERS_IN_OBJECT!>object H<!><T, R><!CONSTRUCTOR_IN_OBJECT!>()<!>
