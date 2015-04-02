import kotlin.util.*
import java.io.*
import java.util.*

fun sample() : Reader {
    return StringReader("""Hello
World""");
  }

  fun box() : String {
    // TODO compiler error
    // both these expressions causes java.lang.NoClassDefFoundError: collections/CollectionPackage
    val list1 = sample().useLines{it.toArrayList()}
    val list2 = sample().useLines<ArrayList<String>>{it.toArrayList()}

    if(arrayList("Hello", "World") != list1) return "fail"
    if(arrayList("Hello", "World") != list2) return "fail"
    return "OK"
  }
