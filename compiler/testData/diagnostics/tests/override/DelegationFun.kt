// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package delegation

interface Aaa {
    fun foo()
}

class Bbb(aaa: Aaa) : Aaa by aaa
