trait Tr { 
    fun foo(): String
}

class DelegateTo : Delegation.ReturnNull(), Tr {
    override fun foo() = super<Delegation.ReturnNull>.foo()
}

class DelegateFrom : Tr by DelegateTo() {
}

fun box(): String {
    try {
        DelegateFrom().foo()
        return "Fail: should have been an exception"
    }
    catch(e: IllegalStateException) {
        println(e.getMessage())
        return "OK"
    }
}