val testGlobal = 12

fun some() {
  val testInFun = 12
}

trait SomeTrait {
  val testInTrait
}

class Some() {
  val testInClass = 12

  default object {
    val testInClassObject = 12
  }
}

// SEARCH_TEXT: test
// REF: (<root>).testGlobal
// REF: (Some).testInClass
// REF: (Some.Default).testInClassObject
// REF: (SomeTrait).testInTrait