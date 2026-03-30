class Props

class Host {
    fun source(a: Int) {}

    fun brokenType(...Props.) {}

    fun brokenFunction(...source.$props.) {}

    fun brokenCall(target: (Int) -> Unit) {
        target(...Props.$props(this).exclude(,))
    }
}
