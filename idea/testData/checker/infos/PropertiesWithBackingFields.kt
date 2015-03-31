<info>abstract</info> class Test() {
    <info>abstract</info> val x : Int
    <info>abstract</info> val x1 : Int <info>get</info>
    <info>abstract</info> val x2 : Int <error><info>get</info>() = 1</error>

    <error>val a : Int</error>
    <error>val b : Int</error> <info>get</info>
    val <info>c</info> = 1

    val <info>c1</info> = 1
      <info>get</info>
    val c2 : Int
        <info>get</info>() = 1
    val c3 : Int
        <info>get</info>() { return 1 }
    val c4 : Int
        <info>get</info>() = 1
    <error>val c5 : Int</error>
        <info>get</info>() = $c5 + 1

    <info>abstract</info> var y : Int
    <info>abstract</info> var y1 : Int <info>get</info>
    <info>abstract</info> var y2 : Int <info>set</info>
    <info>abstract</info> var y3 : Int <info>set</info> <info>get</info>
    <info>abstract</info> var y4 : Int <info>set</info> <error><info>get</info>() = 1</error>
    <info>abstract</info> var y5 : Int <error><info>set</info>(x) {}</error> <error><info>get</info>() = 1</error>
    <info>abstract</info> var y6 : Int <error><info>set</info>(x) {}</error>

    <error>var v : Int</error>
    <error>var v1 : Int</error> <info>get</info>
    <error>var v2 : Int</error> <info>get</info> <info>set</info>
    <error>var v3 : Int</error> <info>get</info>() = 1; <info>set</info>
    var v4 : Int <info>get</info>() = 1; <info>set</info>(x){}

    <error>var v5 : Int</error> <info>get</info>() = 1; <info>set</info>(x){$v5 = x}
    <error>var v6 : Int</error> <info>get</info>() = $v6 + 1; <info>set</info>(x){}

  <error>var v9 : Int</error> <info>set</info>
  <error>var v10 : Int</error> <info>get</info>

}

<info>open</info> class Super(<warning>i</warning> : Int)

class TestPCParameters(w : Int, <warning>x</warning> : Int, val <info>y</info> : Int, var <info>z</info> : Int) : Super(w) {

  val <info>xx</info> = w

  <info>init</info> {
    w + 1
  }

  fun foo() = <error>x</error>

}