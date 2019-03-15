fun f1(a: <!ELEMENT!>) {}

fun f2(a: <!ELEMENT!><*>) {}

fun f3(a: <!ELEMENT!><out *>) {}

fun f4(a: <!ELEMENT!><in List<<!ELEMENT!><*>>>) {}
