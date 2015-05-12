package test

public interface TypeParamOfFun {

    public interface Super {
        public fun <T> foo(): T

        public fun dummy() // to avoid loading as SAM interface
    }

    public interface Sub: Super {
        override fun <E> foo(): E
    }
}
