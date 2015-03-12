 var x : Int = 1 + <error>x</error>
   get() : Int = 1
   set(value : <error>Long</error>) {
      $x = value.toInt()
      $x = <error>1.toLong()</error>
    }

 val xx : Int = <error>1 + x</error>
   get() : Int = 1
   <error>set(value : <error>Long</error>) {}</error>

  val p : Int = <error>1</error>
    get() = 1

class Test() {
    var a : Int = 111
    var b : Int get() = $a; set(x) {a = x; $a = x}

   init {
    <error>$b</error> = $a
    $a = <error>$b</error>
    a = <error>$b</error>
   }
   fun f() {
    <error>$b</error> = $a
    a = <error>$b</error>
   }
}
