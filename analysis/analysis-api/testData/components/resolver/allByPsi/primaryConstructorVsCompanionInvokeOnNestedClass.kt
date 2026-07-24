package com.example

import com.example.Foo.Nested

class Foo {
   class Nested(s: String) {
      companion object {
         operator fun invoke(param: Int) = Nested("str") // constructor
      }
   }
}

fun main() {
   Nested(1)
   Foo.Nested(2)
   com.example.Foo.Nested(3)

   Nested.Companion(4)
   Foo.Nested.Companion(5)
   com.example.Foo.Nested.Companion(6)
}
