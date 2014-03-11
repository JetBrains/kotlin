fun foo() {
    class A {
        var a : Int
          get() {
              return $a
          }
          set(v: Int) {
              $a = v
          }
    }
}
