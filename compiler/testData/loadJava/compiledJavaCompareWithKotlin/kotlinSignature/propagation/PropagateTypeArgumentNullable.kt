package test

public trait PropagateTypeArgumentNullable: Object {

    public trait Super: Object {
        public fun outS(p: List<String?>)

        public fun invOutS(p : MutableList<List<String?>>)

        public fun outOutS(p : List<List<String?>>)

        public fun outR() : List<String?>
        public fun invR() : MutableList<String?>
        public fun invOutR() : MutableList<List<String?>>

    }

    public trait Sub: Super {
        override fun outS(p: List<String?>)

        override fun invOutS(p : MutableList<List<String?>>)

        override fun outOutS(p : List<List<String?>>)

        override fun outR() : List<String?>
        override fun invR() : MutableList<String?>
        override fun invOutR() : MutableList<List<String?>>
    }
}
