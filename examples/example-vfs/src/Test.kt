fun a(op: (Int) -> Int) {}
fun b() {
    a {it}
    a {
      it
    }
    2 + 2
}