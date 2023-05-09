// FIR_IDENTICAL
class A<T> {
    class T

    object E {
        init {
            T()
        }
    }

    class F {
        init {
            T()
        }
    }

    inner class I {
        init {
            T() // todo: https://jetbrains.quip.com/hPM5AJcc1nca
        }
    }

    init {
        T()
    }
}

class B<T> {
    companion object {
        class T;

        init {
            T()
        }
    }

    object E {
        init {
            T()
        }
    }

    class F {
        init {
            T()
        }
    }

    inner class I {
        init {
            T()
        }
    }

    init {
        T()
    }
}