fun f<caret>oo(): Runnable {
    class Local : Runnable {
        override fun run() {}
    }
    return Local()
}
