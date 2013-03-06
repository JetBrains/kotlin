package test

public trait TypeParamOfFun: Object {

    public trait Super: Object {
        public fun <T> foo(): T
    }

    public trait Sub: Super {
        override fun <E> foo(): E
    }
}
