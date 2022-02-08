class Foo

enum class Enum {
    Bar
}

annotation class Ann1(
    val p1: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Int><!>,
    val p2: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Int?><!>,
    val p3: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<IntArray><!>,
    val p4: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Array<Foo><!>,
    val p5: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Foo<!>,
    vararg val p6: <!INVALID_TYPE_OF_ANNOTATION_MEMBER!>Class<*><!>
)

annotation class Ann2(
    val p1: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>Int?<!>,
    val p2: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>String?<!>,
    val p3: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>IntArray?<!>,
    val p4: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>Array<Int>?<!>,
    val p5: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>Ann1?<!>,
    val p6: <!NULLABLE_TYPE_OF_ANNOTATION_MEMBER!>Enum?<!>
)
