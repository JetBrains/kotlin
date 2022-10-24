fun test(v: Any?) {
    (v as? String)?.let{
        <expr>it</expr>.length
    }
}