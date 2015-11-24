package test

class W(val value: Any)

inline fun W.safe(crossinline body : Any.() -> Unit) {
    {
        this.value?.body()
    }()
}

