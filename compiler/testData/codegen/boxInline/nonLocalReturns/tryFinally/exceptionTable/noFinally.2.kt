package test

public inline fun  doCall(block: ()-> String, exception: (e: Exception)-> Unit) : String {
    try {
        return block()
    } catch (e: Exception) {
        exception(e)
    }
    return "Fail in doCall"
}