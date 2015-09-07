import kotlin.jvm.*

external fun foo()

class C {
    external fun foo()

    companion object {
        external fun foo()
    }
}

object O {
    external fun foo()
}

fun test() {
    class Local {
        external fun foo()
    }

    object {
        external fun foo()
    }
}