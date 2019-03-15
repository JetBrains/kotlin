interface Test {
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@get:JvmStatic
    val a: Int<!>

    <!INAPPLICABLE_JVM_NAME!>@get:JvmName("1")<!>
    val b: Int

    <!SYNCHRONIZED_ON_ABSTRACT!>@get:Synchronized<!>
    val c: Int

    <!OVERLOADS_INTERFACE, WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JvmOverloads<!>
    val d: Int
}
