  class Foo(var bar : Int, var barr : Int, var barrr : Int) {
    {
      bar = 1
      barr = 1
      barrr = 1
      1 : Int
      this : Foo
    }

    {
      bar = 1
      this.bar
      1 : Int
      val <warning>a</warning> : Int =1
      this : Foo
    }
  }

