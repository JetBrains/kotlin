class A {

    companion object {

        @JvmStatic const val z = 1;

        @JvmStatic @JvmField val x = 1;
    }

}


object B {

    @JvmStatic const val z = 1;

    @JvmStatic @JvmField val x = 1;
}
