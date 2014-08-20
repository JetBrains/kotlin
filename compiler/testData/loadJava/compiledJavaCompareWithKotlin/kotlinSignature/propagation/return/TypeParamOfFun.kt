package test

public trait TypeParamOfFun {

    public trait Super {
        public fun <T> foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun <E> foo(): E
    }
}
