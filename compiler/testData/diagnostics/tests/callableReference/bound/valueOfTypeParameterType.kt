fun <T: Any> get(t: T): () -> String {
    return t::toString
}
