fun <T> test(obj: T, block: <expr>context(T) () -> Unit</expr>) {
    block(obj)
}