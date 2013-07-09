class StrSome {
  class StrOther

  fun more() {
    class StrInFun

    fun Str<caret>
  }
}

class StrMore {
  class StrAbsent
}

// TIME: 1
// EXIST: String~(jet)
// EXIST: StrSome
// EXIST: StrMore
// EXIST: StrInFun
// EXIST_JAVA_ONLY: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: StrAbsent