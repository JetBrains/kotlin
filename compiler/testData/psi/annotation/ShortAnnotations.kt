foo bar(1) buzz<T>(1) zoo package aa

foo bar(1) buzz<T>(1) zoo class A
foo bar(1) buzz<T>(1) zoo object B
foo bar(1) buzz<T>(1) zoo fun a() {}
foo bar(1) buzz<T>(1) zoo val c : Int = 0
foo bar(1) buzz<T>(1) zoo var v : Int = 0


class Foo {
  foo bar(1) buzz<T>(1) zoo companion object {}
  foo bar(1) buzz<T>(1) zoo class A
  foo bar(1) buzz<T>(1) zoo object B
  foo bar(1) buzz<T>(1) zoo fun a() {}
  foo bar(1) buzz<T>(1) zoo val c : Int = 0
  foo bar(1) buzz<T>(1) zoo var v : Int = 0

  foo bar(1) buzz<T>(1) zoo init {}
}

fun test() {
  when (@foo @bar(1) @buzz<T>(1) @zoo val a = 1) {
    1 -> 1
  }

  when (foo bar(1) buzz<T>(1) zoo val a = 1) {
    1 -> 1
  }
}
