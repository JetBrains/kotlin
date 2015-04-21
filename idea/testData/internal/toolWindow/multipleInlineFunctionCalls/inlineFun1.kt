package inlineFun1

import inlineFun2.*
import inlineFun3.*

inline fun myFun1(f: () -> Int): Int {
    return myFun3 { f() } + myFun2 { f() }
}