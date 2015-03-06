import kotlin.platform.*

class C {
    default object {
        private platformStatic fun foo(): String {
            return "OK"
        }
    }

    fun bar(): String {
        return foo()
    }
}

fun box(): String {
    return C().bar()
}