fun test() {
  var a : Any? = null
  if (a is Any) else a = null;
  while (a is Any) a = null
  while (true) a = <!UNUSED_VALUE!>null<!>
}
