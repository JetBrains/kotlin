package test

class W(val value: Any)

inline fun W.safe(body : Any.() -> Unit) {
    this.value?.body()
}

