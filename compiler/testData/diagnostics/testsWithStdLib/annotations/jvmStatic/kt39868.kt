// FIR_IDENTICAL

// KT-39868: @JvmStatic needs to be applied for protected members in companion objects.
class A {

    companion object {

        @JvmStatic protected const val z = 1;

        @JvmStatic @JvmField protected val x = 1;
    }

}
