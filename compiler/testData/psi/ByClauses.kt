class A : b by a {
  companion object {}
}
class A : b by a + b() * 5 {
  companion object {}
}
class A : b by (a) {
  companion object {}
}
class A : b by (a {}) {
  companion object {}
}
class A : b by a[a {}] {
  companion object {}
}
class A : b by a(a {}) {
  companion object {}
}
class A : b by object {
  fun f() = a {}
} {
  companion object {}
}