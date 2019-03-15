//ALLOW_AST_ACCESS
package test

public class Outer {
      public object Obj {
          public val v: String = { "val" }()
          public fun f(): String = "fun"
      }
}
