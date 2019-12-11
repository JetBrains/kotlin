var x : Int = 1 + x
   get() : Int = 1
   set(value : Long) {
      field = value.toInt()
      field = 1.toLong()
    }

 val xx : Int = 1 + x
   get() : Int = 1
   set(value : Long) {}

  val p : Int = 1
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int = 222
        get() = field
        set(x) {a = x; field = x}

   public val i = 1
}