// RUN_PIPELINE_TILL: SOURCE
// FIR_IDENTICAL
interface A
class AImpl : A

class B : <!UNSUPPORTED!>A by AImpl()<!> {
    constructor()
}