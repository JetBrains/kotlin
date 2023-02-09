
package pkg

private interface ITopLevelPrivate
internal interface ITopLevelInternal
interface ITopLevelPublic

open class Open {
  private class Private: Open() {}
  protected inner class Private2 {}
  internal class StaticInternal {}

  private interface INestedPrivate
  protected interface INestedProtected
  internal interface INestedInternal
  interface INestedPublic
}
internal class OuterInternal {}
private class TopLevelPrivate {}

sealed class Season {
    class Nested: Season()
}

sealed class SealedWithArgs(val a: Int)

// FIR_COMPARISON