// WITH_STDLIB

import kotlin.test.*

fun box(): String {
    main(emptyArray())

    return "OK"
}

fun main(args: Array<String>) {
    val b = args.size < 1
    val x = if (b) Any() else throw Error()
}
