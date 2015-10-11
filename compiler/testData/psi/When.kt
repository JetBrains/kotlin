fun foo() {
  return false

  fun a() = {when(x) { is a -> b }}

  // foo
  fun b() {}

}

fun foo() {
  when (e) {
                          
  }
  when (e) {
    is a -> foo
  }
  when (e) {
    is Tree<a, b> -> foo
    null -> foo
    1 -> foo
    A.b -> foo
    1.0 -> foo
    'c' -> foo
    "sadfsa" -> foo
    """ddd""" -> foo
  }
  when (when(when (e) {

  }) {

  }) {

  }
}

fun foo() {
  when (val a = e) {
    is Tree -> c
    is a.a<a, b> -> c
    is package.a.a<a, b> -> c
    in 1..2 -> dsf
    !in 2 -> sd
    !is t -> d
    (foo) -> Bar -> fgpp
    is Pair<Int, Int> -> 2
    else -> foo
  }
}

fun foo() {
  when (val a = e) {
    is Tree,
    is a<a, b> -> c
    1, foo(), bar, 2 + 3,
    is a<a, b> -> c
  }
}

fun whenWithoutCondition(i : Int) {
  val j = 12
  when {
    3 -> -1
    i == 3 -> -1
    j < i, j == i -> -1
    i is Int -> 1
    else -> 2
  }
}

fun ifDoesntCaptureElse(x : Int) {
  when (x) {
    2 -> if(1) 3
    else -> 6
  }
}