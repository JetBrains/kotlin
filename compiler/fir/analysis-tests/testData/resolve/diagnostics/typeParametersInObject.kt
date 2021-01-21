<!TYPE_PARAMETERS_IN_OBJECT!>object A<!><T, K : T> {
    <!TYPE_PARAMETERS_IN_OBJECT!>object B<!><L>
}

class N {
    companion <!TYPE_PARAMETERS_IN_OBJECT!>object<!><T> {

    }
}

fun test() {
    <!LOCAL_OBJECT_NOT_ALLOWED, TYPE_PARAMETERS_IN_OBJECT!>object M<!><H> {

    }
}
