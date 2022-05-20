fun test() = "test"

val rest = "rest"

fun box() {
    val a = <!INCORRECT_HASH_QUALIFIED_NAME!>10#field<!>
    val b = <!INCORRECT_HASH_QUALIFIED_NAME!>"test"#self<!>
    val c = <!INCORRECT_HASH_QUALIFIED_NAME!>("a" + "b")#delegate<!>
    val d = test()<!SYNTAX!>#field<!>

    with (20) {
        val e = <!INCORRECT_HASH_QUALIFIED_NAME!>this@with#delegate<!>
    }

    val g = ::<!UNSUPPORTED!>rest#field#self#self<!>

    val h = <!INCORRECT_HASH_QUALIFIED_NAME!>rest#10<!>
    val i = <!INCORRECT_HASH_QUALIFIED_NAME!>rest#"test"<!>
    val j = <!INCORRECT_HASH_QUALIFIED_NAME!>rest#("a" + "b")<!>
}
