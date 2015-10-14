class My {
    internal open class ThreadLocal
    // Private from local: ???
    private val values = 
            // Local from internal: Ok
            object: ThreadLocal() {}
}