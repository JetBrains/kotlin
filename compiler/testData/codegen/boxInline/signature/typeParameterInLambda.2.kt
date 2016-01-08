package test

open class Test {

    inline fun <Y> test(z: () -> () -> Y) = z()

    fun <T> callInline(p: T)  = test<T> {
        {
            p
        }
    }
}