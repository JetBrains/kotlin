fun box() : String {
    try {
        return "OK"
    }
    finally {
        null?.toString()
    }
}
