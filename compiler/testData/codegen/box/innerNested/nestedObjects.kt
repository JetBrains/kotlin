object A {
    object B {
        object C {
            val ok = "OK"
        }
    }
}

fun box() = A.B.C.ok
