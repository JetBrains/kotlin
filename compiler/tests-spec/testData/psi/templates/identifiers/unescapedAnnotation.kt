class A {
    @field:<!ELEMENT!>
    val a: Int = ""

    @setparam:<!ELEMENT!>(<!ELEMENT!>)
    val b: Int = ""

    @receiver:org.jetbrains.<!ELEMENT!><A<B, C>>(<!ELEMENT!>)
    val c: Int = ""

    @org.jetbrains.<!ELEMENT!>
    val c: Int = ""

    @<!ELEMENT!><A<B>, C>(<!ELEMENT!>, <!ELEMENT!>, <!ELEMENT!>)
    val c: Int = ""

    @<!ELEMENT!>
    val c: Int = ""

    @<!ELEMENT!>.<!ELEMENT!>.<!ELEMENT!><A<B>, C>(<!ELEMENT!>, <!ELEMENT!>, <!ELEMENT!>)
    val c: Int = ""
}
