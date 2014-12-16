fun foo() {
    when {
        true -> <selection>{
            fun a() = 1
            fun b() = a() + 1
            println(a() - b())
        }</selection>

        true -> {
            fun b() = 1
            fun a() = b() + 1
            println(b() - a())
        }

        true -> {
            fun b() = 1
            fun a() = b() + 1
            println(a() - b())
        }
    }
}