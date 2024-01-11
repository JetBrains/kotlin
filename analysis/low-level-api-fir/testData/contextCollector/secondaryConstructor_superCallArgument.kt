package test

open class Base(i: Int)

val property = 10

class Child : Base {
    constructor(): super(<expr>property</expr>)
}