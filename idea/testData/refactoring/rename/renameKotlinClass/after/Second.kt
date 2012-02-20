package testing.second

import testing.first.Third
import testing.*
import java.util.ArrayList

// Extends testing.first.Third
public class Second : Third() {
  val temp : testing.first.Third = Third()

  fun tempName(param : Third) : first.Third {
    val local = Third()
    val otherLocal = param
    val arr = ArrayList<Third>()

    return testing.first.Third()
  }
}