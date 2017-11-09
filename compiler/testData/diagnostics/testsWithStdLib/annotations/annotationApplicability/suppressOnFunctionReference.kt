// See KT-15839

val x = "1".let(@Suppress("DEPRECATION") Integer::parseInt)