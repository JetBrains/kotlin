
package pkg

open class Open {
  private class Private: Open {}
  protected inner class Private2 {}
  internal class StaticInternal {}
}
internal class OuterInternal {}
private class TopLevelPrivate {}

sealed class Season {
    class Nested: Season()
}

sealed class SealedWithArgs(val a: Int)