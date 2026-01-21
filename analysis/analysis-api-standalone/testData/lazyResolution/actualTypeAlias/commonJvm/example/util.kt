package example

public expect interface AutoCloseable {
    public fun close()
}

public expect interface Closeable : AutoCloseable
