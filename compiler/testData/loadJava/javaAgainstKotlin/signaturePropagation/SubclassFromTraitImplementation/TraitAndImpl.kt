package test

interface Trait {
  fun foo(): String = "foo"

  fun bar(): String = "bar"
}

open class Impl : Trait {
}