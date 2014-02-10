package test

trait A

class <caret>B(val bar: A = object: A {}, val withError = object: A {}, notProperty = object: A {})