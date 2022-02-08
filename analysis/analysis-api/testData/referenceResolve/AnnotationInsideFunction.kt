package test

annotation class Annotation

fun test() {
  @<caret>Annotation fun some() {
  }
}

