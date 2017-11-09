fun foo() {
  f(a)
  g<bar>(a)
  h<baz>
  (a)
  i {s}
  j;
  {s}
  k {
    s
  }
  l(a) {
    s
  }
  m(a);
  {
    s
  }
  n<a>(a) {
    s
  }
  o<a>(a);
  {
    s
  }
  p(qux<a, b>)
  q(quux<a, b>(a))
  r(corge<a, 1, b>(a))
  s(grault<a, (1 + 2), b>(a))
  t(garply<a, 1 + 2, b>(a))
  u(waldo<a, 1 * 2, b>(a))
  v(fred<a, *, b>(a))
  w(plugh<a, "", b>(a))
  xyzzy<*>()
  1._foo()
  1.__foo()
  1_1._foo()
  1._1foo()
  1._1_foo()
}
