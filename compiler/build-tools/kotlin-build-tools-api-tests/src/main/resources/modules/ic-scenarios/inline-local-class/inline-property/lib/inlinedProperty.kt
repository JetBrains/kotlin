val value: Int
    inline get() {
        val lambda = {
            40 + 2
        }
        return lambda()
    }
