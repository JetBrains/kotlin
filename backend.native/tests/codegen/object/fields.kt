class fields(a:Int) {
    public val b:Int
            get() = getB()

    external fun getB():Int
}