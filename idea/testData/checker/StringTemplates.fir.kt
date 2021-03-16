fun demo() {
  val abc = 1
  val a = ""
  val asd = 1
  val bar = 5
  fun map(f : () -> Any?) : Int  = 1
  fun buzz(f : () -> Any?) : Int  = 1
  val sdf = 1
  val foo = 3;
    use("$abc")
    use("$")
    use("$.$.asdf$\t")
    use("asd\$")
    use("asd$a\x")
    use("asd$a$asd$ $<error descr="[UNRESOLVED_REFERENCE] Unresolved reference: xxx">xxx</error>")
    use("fosdfasdo${1 + bar + 100}}sdsdfgdsfsdf")
    use("foo${bar + map {foo}}sdfsdf")
    use("foo${bar + map { "foo" }}sdfsdf")
    use("foo${bar + map {
      "foo$sdf${ buzz{}}" }}sdfsdf")
}

fun use(s: String) {}
