package inlineFun2

import inlineFun3.*

inline fun myFun2(f: () -> Int): Int = myFun3 { f() }