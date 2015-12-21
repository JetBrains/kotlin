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