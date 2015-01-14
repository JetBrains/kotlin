package testing.rename2

import testing.rename.First
import testing.rename
import java.util.ArrayList

// Extends testing.rename.First
public class Second : First() {
  val temp : testing.rename.First = First()

  fun tempName(param : First) : rename.First {
    val local = First()
    val otherLocal = param
    val arr = ArrayList<First>()

    return testing.rename.First()
  }
}