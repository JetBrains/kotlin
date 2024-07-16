fun f(s: String, action: (String.() -> Unit)?) {
    <expr>s.action</expr>?.let { it() }
}

// IGNORE_STABILITY_K1: candidates
// FunctionInvokeDescriptor are not stable