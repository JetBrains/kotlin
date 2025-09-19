// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-79330
// LANGUAGE: +CollectionLiterals

class MyList<T> {
    companion object {
        operator fun <T> of(vararg vals: T): MyList<T> = MyList<T>()
    }
}

class A

fun test() {
    val lst1: MyList<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    val lst2: MyList<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst3: MyList<String?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst4: MyList<Any?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!>
    val lst5: MyList<Any?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, A(), "0"]<!>
    val lst6: MyList<String> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, "0"]<!> // should not pass
    val lst7: MyList<A?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null, "0", A()]<!> // should not pass
    val lst8: MyList<Nothing> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[]<!>
    val lst9: MyList<Nothing> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>["1", "2", "3"]<!> // should not pass
    val lst10: MyList<Nothing?> = <!INITIALIZER_TYPE_MISMATCH, UNSUPPORTED!>[null]<!>
}
