// LANGUAGE: +FullValueClasses
package one

/* ClassId: one/Point */value class Point(val x: Int, val y: Int) {
    /* ClassId: one/Point.Nested */class Nested

    /* ClassId: one/Point.NestedValue */value class NestedValue(val value: Int, val other: Int)

    /* ClassId: one/Point.Companion */companion object
}

/* ClassId: one/Empty */value object Empty
