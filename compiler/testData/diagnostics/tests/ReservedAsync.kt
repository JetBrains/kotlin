fun async(f: () -> Unit) {
    f()
}

infix fun Any.async(f: () -> Unit) = f()

fun test(foo: Any) {
    <!UNSUPPORTED!>async<!> {  }
    `async` {  }
    <!UNSUPPORTED!>async<!> /**/ {  }
    foo <!UNSUPPORTED!>async<!> {  }

    async() { }

    async({ })
    foo async ({ })

    foo <!UNSUPPORTED!>async<!> fun () {}
    foo `async` fun () {}
    foo async (fun () {})

    async (fun () {})
}

object async {
    operator fun plus(f: () -> Unit) = f()
    operator fun minus(f: () -> Unit) = f()
    operator fun times(f: () -> Unit) = f()
    operator fun div(f: () -> Unit) = f()
    operator fun mod(f: () -> Unit) = f()
}

fun test() {
    <!UNSUPPORTED!>async<!>+ {}
    <!UNSUPPORTED!>async<!>- {}
    <!UNSUPPORTED!>async<!>* {}
    <!UNSUPPORTED!>async<!>/ {}
    <!UNSUPPORTED!>async<!>% {}

    async + {}
    async - {}
    async * {}
    async / {}
    async % {}
}