fun foo() {
    when {
        true -> <selection>{
            val a = 1
            val b = a + 1
            println(a - b)
        }</selection>

        true -> {
            val b = 1
            val a = b + 1
            println(b - a)
        }

        true -> {
            val b = 1
            val a = b + 1
            println(a - b)
        }
    }
}