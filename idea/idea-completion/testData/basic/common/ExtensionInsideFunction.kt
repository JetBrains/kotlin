class StrSome {
  class StrOther

  fun more() {
    class StrInFun

    fun Str<caret>
  }
}

class StrMore {
  class StrInner
}

// INVOCATION_COUNT: 1
// EXIST: { lookupString:"String", tailText:" (kotlin)" }
// EXIST: StrSome
// EXIST: StrMore
// EXIST: StrInFun
// EXIST: StringBuilder
// EXIST_JAVA_ONLY: StringBuffer
// EXIST: StrInner