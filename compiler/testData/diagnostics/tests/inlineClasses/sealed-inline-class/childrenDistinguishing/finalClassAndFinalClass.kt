// LANGUAGE: +SealedInlineClasses
// SKIP_TXT
// !SKIP_JAVAC

package kotlin.jvm

annotation class JvmInline

/*
+--------------+--------------+--------------+
| First        | Second       | Result       |
+==============+==============+==============+
| String       | String       | Error        |
+--------------+--------------+--------------+
| String       | String?      | Error        |
+--------------+--------------+--------------+
| String       | Int          | OK           |
+--------------+--------------+--------------+
| Int          | Int          | Error        |
+--------------+--------------+--------------+
| Int          | Int?         | Error        |
+--------------+--------------+--------------+
| Gen<Int>     | Gen<String>  | Error        |
+--------------+--------------+--------------+
| IC           | IC?          | Error        |
+--------------+--------------+--------------+
*/

@JvmInline
sealed value class String_String {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>String<!>): String_String()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>String<!>): String_String()
}

@JvmInline
sealed value class String_StringN {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>String<!>): String_StringN()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>String?<!>): String_StringN()
}

@JvmInline
sealed value class String_Int {
    @JvmInline
    value class First(val a: String): String_Int()

    @JvmInline
    value class Second(val a: Int): String_Int()
}

@JvmInline
sealed value class Int_Int {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Int<!>): Int_Int()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Int<!>): Int_Int()
}

@JvmInline
sealed value class Int_IntN {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Int<!>): Int_IntN()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Int?<!>): Int_IntN()
}

class Gen<T>

@JvmInline
sealed value class Gen_Gen {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Gen<Int><!>): Gen_Gen()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>Gen<String><!>): Gen_Gen()
}

@JvmInline
value class IC(val a: String)

@JvmInline
sealed value class IC_IC {
    @JvmInline
    value class First(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>IC<!>): IC_IC()

    @JvmInline
    value class Second(val a: <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>IC?<!>): IC_IC()
}
