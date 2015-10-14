package test

class MembersReferenceOuterTP<P> {
    inner class Inner {
        fun f<Q : P>() {}
        fun g(p: P): P = null!!

        val v: P = null!!
        val <Q : P> Q.w: Q get() = null!!
    }
}
