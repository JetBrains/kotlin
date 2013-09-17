class A : b by a {
  class object {}
}
class A : b by a + b() * 5 {
  class object {}
}
class A : b by (a) {
  class object {}
}
class A : b by (a {}) {
  class object {}
}
class A : b by a[a {}] {
  class object {}
}
class A : b by a(a {}) {
  class object {}
}
class A : b by object {
  fun f() = a {}
} {
  class object {}
}