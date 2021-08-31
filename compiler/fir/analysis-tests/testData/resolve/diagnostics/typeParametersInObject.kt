object A<!TYPE_PARAMETERS_IN_OBJECT!><T, K : T><!> {
    object B<!TYPE_PARAMETERS_IN_OBJECT!><L><!>
}

class N {
    companion object<!TYPE_PARAMETERS_IN_OBJECT!><T><!> {

    }
}

fun test() {
    <!LOCAL_OBJECT_NOT_ALLOWED!>object M<!><!TYPE_PARAMETERS_IN_OBJECT!><H><!> {

    }
}
