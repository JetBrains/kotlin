// ISSUE: KT-70352

fun <T: <!FINAL_UPPER_BOUND!>Nothing<!>> f() {}
