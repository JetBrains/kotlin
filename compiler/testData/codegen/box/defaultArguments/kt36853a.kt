tailrec fun tailrecDefault(fake: Int, fn: () -> String = { "OK" }): String {
    return if (fake == 0)
        tailrecDefault(1)
    else
        fn()
}

fun box(): String = tailrecDefault(0)
