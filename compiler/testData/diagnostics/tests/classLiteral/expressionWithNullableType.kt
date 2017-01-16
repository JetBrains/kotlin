// FILE: J.java

public interface J {
    String platformString();
}

// FILE: test.kt

fun f1(x: Int?): Any = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>x<!>::class
fun <T> f2(t: T): Any = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>t<!>::class
fun <S : String?> f3(s: S): Any = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>s<!>::class
fun <U : Any> f4(u: U?): Any = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>u<!>::class
fun f5(c: List<*>): Any = <!EXPRESSION_OF_NULLABLE_TYPE_IN_CLASS_LITERAL_LHS!>c[0]<!>::class

fun f6(j: J): Any = j.platformString()::class
