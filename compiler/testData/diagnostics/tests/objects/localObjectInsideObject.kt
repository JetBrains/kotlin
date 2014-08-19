fun foo() {
    val a = object {
        val b = object {
            val c = 42
        }
    }

    a.b.c : Int
}