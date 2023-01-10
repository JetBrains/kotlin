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
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: String): String_String()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: String): String_String()
}

@JvmInline
sealed value class String_StringN {
    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: String): String_StringN()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: String?): String_StringN()
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
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: Int): Int_Int()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: Int): Int_Int()
}

@JvmInline
sealed value class Int_IntN {
    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: Int): Int_IntN()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: Int?): Int_IntN()
}

class Gen<T>

@JvmInline
sealed value class Gen_Gen {
    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: Gen<Int>): Gen_Gen()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: Gen<String>): Gen_Gen()
}

@JvmInline
value class IC(val a: String)

@JvmInline
sealed value class IC_IC {
    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class First(val a: IC): IC_IC()

    @JvmInline
    <!SEALED_INLINE_CHILD_OVERLAPPING_TYPE!>value<!> class Second(val a: IC?): IC_IC()
}
