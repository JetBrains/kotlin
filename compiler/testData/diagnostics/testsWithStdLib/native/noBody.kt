import kotlin.jvm.*

native fun foo()

class C {
    native fun foo()

    default object {
        native fun foo()
    }
}

object O {
    native fun foo()
}

fun test() {
    class Local {
        native fun foo()
    }

    object {
        native fun foo()
    }
}