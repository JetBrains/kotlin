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
            <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>T<!>() // todo: fix error message
        }
    }

    init {
        <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>T<!>()
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
            <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>T<!>()
        }
    }

    init {
        <!NESTED_CLASS_SHOULD_BE_QUALIFIED!>T<!>()
    }
}