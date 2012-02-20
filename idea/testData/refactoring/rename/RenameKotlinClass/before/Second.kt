package testing.second

import testing.first.First
import testing.*
import java.util.ArrayList

// Extends testing.first.First
public class Second : First() {
  val temp : testing.first.First = First()

  fun tempName(param : First) : first.First {
    val local = First()
    val otherLocal = param
    val arr = ArrayList<First>()

    return testing.first.First()
  }
}