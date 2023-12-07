package test

open class Base(param: Nested) {
    class Nested
}

val property: Base.Nested = Base.Nested()

val child = object : Base(<expr>property</expr>) {}