package com.example

open class Base

interface SomeInterface

class Derived : Base(), SomeInterface

private class PrivateClass

class Outer {
    class PublicNested

    private class PrivateNested {
        class NestedInPrivate
    }
}
