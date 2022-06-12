fun f() {
    call<>
    call<x.>
    call<>()
    call<x.>()
    call<> { }
    call<x.> { }
    call<>::
    call<x.>::
    call<>[]
    call<x.>[]
    call<>?
    call<x.>?
    call<>?.
    call<x.>?.
    call<>.
    call<x.>.
    call<>++
    call<x.>++
    call<>--
    call<x.>--
    call<>!!
    call<x.>!!

    a(call<>())
    a(call<x.>())
    a(call<> { })
    a(call<x.> { })
    a(call<>::)
    a(call<x.>::)
    a(call<>[])
    a(call<x.>[])
    a(call<>?)
    a(call<x.>?)
    a(call<>?.)
    a(call<x.>?.)
    a(call<>.)
    a(call<x.>.)
    a(call<>++)
    a(call<x.>++)
    a(call<>--)
    a(call<x.>--)
    a(call<>!!)
    a(call<x.>!!)
}