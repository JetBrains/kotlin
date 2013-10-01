fun foo() {
  val a = a + b
  val a = a +
    b
  val a = a
  + b
  val a = (a
  + b)
  val a = ({a
  + b})
  val a = ({a
  + b}
  + b)

  val a = b[c
    + d]
  val a = b[{c
    + d}]
  val a = b[{c
    + d}
    + d]

  when (e) {
    is T
    <X>
    -> a
    in f
    () -> a
    !is T
    <X> -> a
    !in f
    () -> a
    f
    () -> a
  }
  val f = a is T
  <X>
}