// LATEST_LV_DIFFERENCE

fun x(): Boolean { return true }

public fun foo(pp: String?): Int {
    var p = pp
    while(true) {
        p!!.length
        if (x()) break
        <!WRAPPED_LHS_IN_ASSIGNMENT_WARNING!>(((p)))<!> = null
    }
    // Smart cast is NOT possible here
    // (we could provide it but p = null makes it much harder)
    return p.length
}