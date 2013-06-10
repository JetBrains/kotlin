package foo

fun dispatch(request: Request) {
    val <!UNUSED_VARIABLE!>url<!> = request.getRequestURI() as String

    if (request.getMethod()?.length != 0) {
    }
}

trait Request {
    fun getRequestURI(): String?
    fun getMethod(): String?
}
