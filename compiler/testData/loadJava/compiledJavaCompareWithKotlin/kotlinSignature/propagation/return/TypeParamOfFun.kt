package test

public trait TypeParamOfFun: Object {

    public trait Super: Object {
        public fun <T> foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public trait Sub: Super {
        override fun <E> foo(): E
    }
}
