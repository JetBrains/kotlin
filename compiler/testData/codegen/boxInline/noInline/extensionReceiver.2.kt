inline fun (() -> String).test(): (() -> String) = { invoke() + this.invoke() + this() }

// call this.hashCode() guarantees that extension receiver is noinline by default
inline fun (() -> String).extensionNoInline(): String = this() + (this.hashCode().toString())
