// !WITH_NEW_INFERENCE
// See KT-15839

val x = "1".let(@<!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>Suppress<!>("DEPRECATION") Integer::parseInt)
