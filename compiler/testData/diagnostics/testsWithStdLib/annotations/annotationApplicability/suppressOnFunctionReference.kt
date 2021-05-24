// See KT-15839

val x = "1".let(@<!DEBUG_INFO_MISSING_UNRESOLVED!>Suppress<!>("DEPRECATION") Integer::parseInt)
