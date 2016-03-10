package b

import a.A

interface B {
    fun returnType(): A
    fun parameter(param: A?)
}

fun A?.extensionReceiver() {}
