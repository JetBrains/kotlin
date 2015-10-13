fun foo() {
  b().x
  x++ . 4
  ++ -- ! a
  f(foo<a, a, b>(x))
  f(foo<a, 1, b>(x))
  f(foo<a, (a + 1), b>(x))
  f(foo<a, (b>(x)))
  f(foo<a, b>(x))
  f((foo<a), b>(x))
  f(foo<a, b)
  a + b
  a + b * c
  a + (b * c)
  (a + b) * c
  a + b > c * c
  a + b in c * d
  a.b
  a + b.c
  a.b + c
  a.b++
  --a.b++
  --a * b
  a+b..b-1
  1..2 foo 2..3
  1 foo 2 ?: 1 bar 3
  a b c d e f g
  a ?: b in b?: c
  a < b == b > c
  a != b && c
  a || b && c
  a = b -> c
  a = b || c

  t as Any<T>?
  t as Any.Any<T>.Any<T>
  t as  () -> T
  t as? Any<T>?
  t as? Any.Any<T>.Any<T>
  t as?  () -> T

  t as Any<T>? * 1
  t as Any.Any<T>.Any<T> * 1
  t as  () -> T * 1
  t as? Any<T>? * 1
  t as? Any.Any<T>.Any<T> * 1
  t as?  () -> T * 1
}
