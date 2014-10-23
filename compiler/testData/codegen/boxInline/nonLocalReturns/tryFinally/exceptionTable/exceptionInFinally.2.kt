package test

public trait MCloseable {
    public open fun close()
}

public inline fun <T : MCloseable, R> T.muse(block: (T) -> R): R {
    try {
        return block(this)
    } finally {
        this.close()
    }
}


