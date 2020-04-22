class A<T> {
    fun foo() {
        val q = object {
            open inner class B
            inner class C : B()

            // No WRONG_NUMBER_OF_TYPE_ARGUMENTS should be reported on these types
            val <!EXPOSED_PROPERTY_TYPE!>x<!>: B = B()
            val <!EXPOSED_PROPERTY_TYPE!>y<!>: C = C()
        }

        q.x
        q.y
    }
}
