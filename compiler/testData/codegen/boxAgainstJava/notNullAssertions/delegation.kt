// IGNORE_BACKEND: JVM_IR
// FILE: Delegation.java

public class Delegation {
    public static class ReturnNull {
        public String foo() {
            return null;
        }
    }
}

// FILE: 1.kt

interface Tr {
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
        println(e.message)
        return "OK"
    }
}
