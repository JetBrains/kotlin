// IDENTICAL_KLIB_SYNTHETIC_ACCESSOR_DUMPS

open class OuterOnlyInternal {
    fun foo() = "foo"
    open inner class Inner {
        internal inline fun inlineFoo1() = foo()
        internal inline fun inlineFoo2() = foo()
    }
}

open class OuterInternalAndPublic {
    fun foo() = "foo"
    open inner class Inner {
        internal inline fun inlineFoo1() = foo()
        public inline fun inlineFoo2() = foo()
    }
}

open class OuterInternalAndProtected {
    fun foo() = "foo"
    open inner class Inner {
        internal inline fun inlineFoo1() = foo()
        protected inline fun inlineFoo2() = foo()
    }
}

open class OuterInternalAndInternalPA {
    fun foo() = "foo"
    open inner class Inner {
        internal inline fun inlineFoo1() = foo()
        @PublishedApi internal inline fun inlineFoo2() = foo()
    }
}

open class OuterOnlyPublic {
    fun foo() = "foo"
    open inner class Inner {
        public inline fun inlineFoo1() = foo()
        public inline fun inlineFoo2() = foo()
    }
}

open class OuterOnlyProtected {
    fun foo() = "foo"
    open inner class Inner {
        protected inline fun inlineFoo1() = foo()
        protected inline fun inlineFoo2() = foo()
    }
}

open class OuterOnlyInternalPA {
    fun foo() = "foo"
    open inner class Inner {
        @PublishedApi internal inline fun inlineFoo1() = foo()
        @PublishedApi internal inline fun inlineFoo2() = foo()
    }
}

open class OuterAllEffectivelyPublic {
    fun foo() = "foo"
    open inner class Inner {
        public inline fun inlineFoo1() = foo()
        protected inline fun inlineFoo2() = foo()
        @PublishedApi internal inline fun inlineFoo3() = foo()
    }
}

fun box(): String = "OK"
