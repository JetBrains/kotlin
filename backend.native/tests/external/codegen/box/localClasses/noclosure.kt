fun box(): String {
    open class K {
        val o = "O"
    }

    class Bar : K() {
        val k = "K"
    }

    return K().o + Bar().k
}
