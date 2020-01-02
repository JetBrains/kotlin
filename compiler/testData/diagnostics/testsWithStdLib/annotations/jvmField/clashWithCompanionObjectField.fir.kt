class A {

    @JvmField val clash = 1;

    companion object {
        val clash = 1;
    }
}