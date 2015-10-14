// KT-3559 Strange inference failure error message

public inline fun <T:Any, R> let(subj: T?, body: (T) -> R): R? {
    return if (subj != null) body(<!DEBUG_INFO_SMARTCAST!>subj<!>) else null
}


fun test(s: String?): String? {
    return let(s) {s} // Reports: "Inference failed. Expected jet.String? but found jet.String?"
}