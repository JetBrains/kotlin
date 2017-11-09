val testGlobal = 12

fun some() {
  val testInFun = 12
}

interface SomeTrait {
  val testInTrait
}

class Some() {
  val testInClass = 12

  companion object {
    val testInClassObject = 12
  }
}

class SomePrimary(val testInPrimary: Int)

// SEARCH_TEXT: test
// REF: (<root>).testGlobal
// REF: (in Some).testInClass
// REF: (in Some.Companion).testInClassObject
// REF: (in SomePrimary).testInPrimary
// REF: (in SomeTrait).testInTrait