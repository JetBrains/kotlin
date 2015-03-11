class A : b by a {
  default object {}
}
class A : b by a + b() * 5 {
  default object {}
}
class A : b by (a) {
  default object {}
}
class A : b by (a {}) {
  default object {}
}
class A : b by a[a {}] {
  default object {}
}
class A : b by a(a {}) {
  default object {}
}
class A : b by object {
  fun f() = a {}
} {
  default object {}
}