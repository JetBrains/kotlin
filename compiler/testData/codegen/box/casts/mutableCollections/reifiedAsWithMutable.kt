// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63828

class Itr : Iterator<String> by ArrayList<String>().iterator()
class MItr : MutableIterator<String> by ArrayList<String>().iterator()
class LItr : ListIterator<String> by ArrayList<String>().listIterator()
class MLItr : MutableListIterator<String> by ArrayList<String>().listIterator()

class It : Iterable<String> by ArrayList<String>()
class MIt : MutableIterable<String> by ArrayList<String>()
class C : Collection<String> by ArrayList<String>()
class MC : MutableCollection<String> by ArrayList<String>()
class L : List<String> by ArrayList<String>()
class ML : MutableList<String> by ArrayList<String>()
class S : Set<String> by HashSet<String>()
class MS : MutableSet<String> by HashSet<String>()

class M : Map<String, String> by HashMap<String, String>()
class MM : MutableMap<String, String> by HashMap<String, String>()

class ME : Map.Entry<String, String> {
    override val key: String get() = throw UnsupportedOperationException()
    override val value: String get() = throw UnsupportedOperationException()
}

class MME : MutableMap.MutableEntry<String, String> {
    override val key: String get() = throw UnsupportedOperationException()
    override val value: String get() = throw UnsupportedOperationException()
    override fun setValue(value: String): String = throw UnsupportedOperationException()
}

inline fun <reified T> reifiedAsSucceeds(x: Any, operation: String) {
    try {
        x as T        
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }    
}

inline fun <reified T> reifiedAsFailsWithCCE(x: Any, operation: String) {
    try {
        x as T
    }
    catch (e: ClassCastException) {
        return
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should throw ClassCastException, got $e")
    }
    throw AssertionError("$operation: should fail with CCE, no exception thrown")
}

fun box(): String {
    val itr = Itr() as Any
    val mitr = MItr()

    reifiedAsFailsWithCCE<MutableIterator<*>>(itr, "reifiedAs<MutableIterator<*>>(itr)")
    reifiedAsSucceeds<MutableIterator<*>>(mitr, "reifiedAs<MutableIterator<*>>(mitr)")

    val litr = LItr() as Any
    val mlitr = MLItr()

    reifiedAsFailsWithCCE<MutableIterator<*>>(litr, "reifiedAs<MutableIterator<*>>(litr)")
    reifiedAsFailsWithCCE<MutableListIterator<*>>(litr, "reifiedAs<MutableListIterator<*>>(litr)")
    reifiedAsSucceeds<MutableListIterator<*>>(mlitr, "reifiedAs<MutableListIterator<*>>(mlitr)")

    val it = It() as Any
    val mit = MIt()
    val arrayList = ArrayList<String>()

    reifiedAsFailsWithCCE<MutableIterable<*>>(it, "reifiedAs<MutableIterable<*>>(it)")
    reifiedAsSucceeds<MutableIterable<*>>(mit, "reifiedAs<MutableIterable<*>>(mit)")
    reifiedAsSucceeds<MutableIterable<*>>(arrayList, "reifiedAs<MutableIterable<*>>(arrayList)")

    val coll = C() as Any
    val mcoll = MC()

    reifiedAsFailsWithCCE<MutableCollection<*>>(coll, "reifiedAs<MutableCollection<*>>(coll)")
    reifiedAsFailsWithCCE<MutableIterable<*>>(coll, "reifiedAs<MutableIterable<*>>(coll)")
    reifiedAsSucceeds<MutableCollection<*>>(mcoll, "reifiedAs<MutableCollection<*>>(mcoll)")
    reifiedAsSucceeds<MutableIterable<*>>(mcoll, "reifiedAs<MutableIterable<*>>(mcoll)")
    reifiedAsSucceeds<MutableCollection<*>>(arrayList, "reifiedAs<MutableCollection<*>>(arrayList)")

    val list = L() as Any
    val mlist = ML()

    reifiedAsFailsWithCCE<MutableList<*>>(list, "reifiedAs<MutableList<*>>(list)")
    reifiedAsFailsWithCCE<MutableCollection<*>>(list, "reifiedAs<MutableCollection<*>>(list)")
    reifiedAsFailsWithCCE<MutableIterable<*>>(list, "reifiedAs<MutableIterable<*>>(list)")
    reifiedAsSucceeds<MutableList<*>>(mlist, "reifiedAs<MutableList<*>>(mlist)")
    reifiedAsSucceeds<MutableCollection<*>>(mlist, "reifiedAs<MutableCollection<*>>(mlist)")
    reifiedAsSucceeds<MutableIterable<*>>(mlist, "reifiedAs<MutableIterable<*>>(mlist)")
    reifiedAsSucceeds<MutableList<*>>(arrayList, "reifiedAs<MutableList<*>>(arrayList)")

    val set = S() as Any
    val mset = MS()
    val hashSet = HashSet<String>()

    reifiedAsFailsWithCCE<MutableSet<*>>(set, "reifiedAs<MutableSet<*>>(set)")
    reifiedAsFailsWithCCE<MutableCollection<*>>(set, "reifiedAs<MutableCollection<*>>(set)")
    reifiedAsFailsWithCCE<MutableIterable<*>>(set, "reifiedAs<MutableIterable<*>>(set)")
    reifiedAsSucceeds<MutableSet<*>>(mset, "reifiedAs<MutableSet<*>>(mset)")
    reifiedAsSucceeds<MutableCollection<*>>(mset, "reifiedAs<MutableCollection<*>>(mset)")
    reifiedAsSucceeds<MutableIterable<*>>(mset, "reifiedAs<MutableIterable<*>>(mset)")
    reifiedAsSucceeds<MutableSet<*>>(hashSet, "reifiedAs<MutableSet<*>>(hashSet)")
    reifiedAsSucceeds<MutableCollection<*>>(hashSet, "reifiedAs<MutableCollection<*>>(hashSet)")
    reifiedAsSucceeds<MutableIterable<*>>(hashSet, "reifiedAs<MutableIterable<*>>(hashSet)")

    val map = M() as Any
    val mmap = MM()
    val hashMap = HashMap<String, String>()

    reifiedAsFailsWithCCE<MutableMap<*, *>>(map, "reifiedAs<MutableMap<*, *>>(map)")
    reifiedAsSucceeds<MutableMap<*, *>>(mmap, "reifiedAs<MutableMap<*, *>>(mmap)")
    reifiedAsSucceeds<MutableMap<*, *>>(hashMap, "reifiedAs<MutableMap<*, *>>(hashMap)")

    val entry = ME() as Any
    val mentry = MME()

    hashMap[""] = ""
    val hashMapEntry = hashMap.entries.first()

    reifiedAsFailsWithCCE<MutableMap.MutableEntry<*, *>>(entry, "reifiedAs<MutableMap.MutableEntry<*, *>>(entry)")
    reifiedAsSucceeds<MutableMap.MutableEntry<*, *>>(mentry, "reifiedAs<MutableMap.MutableEntry<*, *>>(mentry)")
    reifiedAsSucceeds<MutableMap.MutableEntry<*, *>>(hashMapEntry, "reifiedAs<MutableMap.MutableEntry<*, *>>(hashMapEntry)")

    return "OK"
}
