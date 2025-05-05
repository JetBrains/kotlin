// DUMP_IR

fun box() : String {
    try {
        return "OK"
    }
    finally {
        null?.toString()
    }
}
