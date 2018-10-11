// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt

package test

interface IZ {
    fun z()
}

interface IZZ : IZ {
    fun zz()
}

inline fun implZZ(zImpl: IZ, crossinline zzImpl: () -> Unit): IZZ =
        object : IZZ, IZ by zImpl {
            override fun zz() = zzImpl()
        }


// FILE: 2.kt

import test.*

var result = "fail";

object ZImpl : IZ {
    override fun z() {
        result = "O"
    }
}

fun box(): String {
    val zz = implZZ(ZImpl) { result += "K" }
    zz.z()
    zz.zz()
    return result
}

