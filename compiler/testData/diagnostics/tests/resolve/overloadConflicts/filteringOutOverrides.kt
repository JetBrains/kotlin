// ISSUE: KT-58524

interface MyGenericInterface<T> {
    fun update(f: (T) -> T) {}
}

interface SubGenericInterface<F> : MyGenericInterface<F> {
    override fun update(f: (F) -> F) {}
}

interface SubInterfaceInt : SubGenericInterface<Int> {
    override fun update(f: (Int) -> Int) {}
}

fun foo1(a: MyGenericInterface<Number>) {
    a <!UNCHECKED_CAST!>as MyGenericInterface<Int><!>

    <!DEBUG_INFO_SMARTCAST!>a<!>.update { expectInt(it) }
}

fun foo2(a: MyGenericInterface<Number>) {
    a <!UNCHECKED_CAST!>as SubGenericInterface<Int><!>

    <!DEBUG_INFO_SMARTCAST!>a<!>.update { expectInt(it) }
}

fun foo3(a: MyGenericInterface<Number>) {
    a as SubInterfaceInt

    <!DEBUG_INFO_SMARTCAST!>a<!>.update { expectInt(it) }
}

fun expectInt(w: Int): Int = w
