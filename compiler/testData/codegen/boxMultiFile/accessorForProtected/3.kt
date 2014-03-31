package b

import a.A

class B: A() {
    fun test(): String {
        val a = {
            protectedFun()
        }
        return a()
    }
}
