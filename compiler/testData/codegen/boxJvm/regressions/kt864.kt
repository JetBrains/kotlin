// TARGET_BACKEND: JVM

// WITH_STDLIB
// FULL_JDK

import java.io.*
import java.util.*

fun sample() : Reader {
    return StringReader("""Hello
World""");
  }

fun box() : String {
  // NOTE: Also tested in stdlib: LineIteratorTest.useLines

  // TODO compiler error
  // both these expressions causes java.lang.NoClassDefFoundError: collections/CollectionPackage
  val list1 = sample().useLines { it.toList() }
  val list2 = sample().useLines<ArrayList<String>>{ it.toCollection(arrayListOf()) }

  if(arrayListOf("Hello", "World") != list1) return "fail"
  if(arrayListOf("Hello", "World") != list2) return "fail"
  return "OK"
}
