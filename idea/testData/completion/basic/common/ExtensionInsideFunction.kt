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

// INVOCATION_COUNT: 1
// EXIST: String~(jet)
// EXIST: StrSome
// EXIST: StrMore
// EXIST: StrInFun
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// ABSENT: StrAbsent