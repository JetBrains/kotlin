fun f() {
    call<x>
    call<x>()
    call<x> { }
    call<x>::
    call<x>[]
    call<x>?
    call<x>?.
    call<x>.
    call<x>++
    call<x>--
    call<x>!!

    a(call<x>())
    a(call<x> { })
    a(call<x>::)
    a(call<x>[])
    a(call<x>?)
    a(call<x>?.)
    a(call<x>.)
    a(call<x>++)
    a(call<x>--)
    a(call<x>!!)
}