interface Test {
    <!JVM_STATIC_NOT_IN_OBJECT_OR_COMPANION!>@get:JvmStatic
    val a: Int<!>

    @get:JvmName("1")
    val b: Int

    @get:Synchronized
    val c: Int

    <!WRONG_ANNOTATION_TARGET_WITH_USE_SITE_TARGET!>@get:JvmOverloads<!>
    val d: Int
}
