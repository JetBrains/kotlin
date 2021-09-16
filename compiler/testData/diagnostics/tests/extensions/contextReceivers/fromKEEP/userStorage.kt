// !LANGUAGE: +ContextReceivers

class Storage<T> {
    inner class Info

    fun info(name: String) = Info()
}
class User
class Logger {
    fun info(message: String) {}
}

context(Logger, Storage<User>)
fun userInfo(name: String): Storage<User>.Info? {
    this@Logger.info("Retrieving info about $name")
    return this@Storage.info(name)
}