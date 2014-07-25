fun demo() {
  val abc = 1
  val a = ""
  val asd = 1
  val bar = 5
  fun map(<!UNUSED_PARAMETER!>f<!> :  () -> Any?) : Int  = 1
  fun buzz(<!UNUSED_PARAMETER!>f<!> :  () -> Any?) : Int  = 1
  val sdf = 1
  val foo = 3;
    <!UNUSED_EXPRESSION!>"$abc"<!>
    <!UNUSED_EXPRESSION!>"$"<!>
    <!UNUSED_EXPRESSION!>"$.$.asdf$\t"<!>
    <!UNUSED_EXPRESSION!>"asd\$"<!>
    <!UNUSED_EXPRESSION!>"asd$a<!ILLEGAL_ESCAPE!>\x<!>"<!>
    "asd$a$asd$ $<!UNRESOLVED_REFERENCE!>xxx<!>"
    "fosdfasdo${1 + bar + 100}}sdsdfgdsfsdf"
    "foo${bar + map {foo}}sdfsdf"
    "foo${bar + map { "foo" }}sdfsdf"
    "foo${bar + map {
      "foo$sdf${ buzz{}}" }}sdfsdf"
    <!UNUSED_EXPRESSION!>"a<!ILLEGAL_ESCAPE!>\u<!> <!ILLEGAL_ESCAPE!>\u<!>0 <!ILLEGAL_ESCAPE!>\u<!>00 <!ILLEGAL_ESCAPE!>\u<!>000 \u0000 \u0AaA <!ILLEGAL_ESCAPE!>\u<!>0AAz.length( ) + \u0022b"<!>
}