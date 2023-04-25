// WITH_STDLIB

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

inline fun asFailsWithCCE(operation: String, block: () -> Unit) {
    try {
        block()
    }
    catch (e: ClassCastException) {
        return
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should throw ClassCastException, got $e")
    }
    throw AssertionError("$operation: should throw ClassCastException, no exception thrown")
}

inline fun asSucceeds(operation: String, block: () -> Unit) {
    try {
        block()
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

fun box(): String {
    val itr = Itr() as Any
    val mitr = MItr()

    asFailsWithCCE("itr as MutableIterator") { itr as MutableIterator<*> }
    asSucceeds("mitr as MutableIterator") { mitr as MutableIterator<*> }

    val litr = LItr() as Any
    val mlitr = MLItr()

    asFailsWithCCE("litr as MutableIterator") { litr as MutableIterator<*> }
    asFailsWithCCE("litr as MutableListIterator") { litr as MutableListIterator<*> }
    asSucceeds("mlitr as MutableIterator") { mlitr as MutableIterator<*> }
    asSucceeds("mlitr as MutableListIterator") { mlitr as MutableListIterator<*> }

    val it = It() as Any
    val mit = MIt()
    val arrayList = ArrayList<String>()

    asFailsWithCCE("it as MutableIterable") { it as MutableIterable<*> }
    asSucceeds("mit as MutableIterable") { mit as MutableIterable<*> }
    asSucceeds("arrayList as MutableIterable") { arrayList as MutableIterable<*> }

    val coll = C() as Any
    val mcoll = MC()

    asFailsWithCCE("coll as MutableIterable") { coll as MutableIterable<*> }
    asFailsWithCCE("coll as MutableCollection") { coll as MutableCollection<*> }
    asSucceeds("mcoll as MutableIterable") { mcoll as MutableIterable<*> }
    asSucceeds("mcoll as MutableCollection") { mcoll as MutableCollection<*> }
    asSucceeds("arrayList as MutableCollection") { arrayList as MutableCollection<*> }

    val list = L() as Any
    val mlist = ML()

    asFailsWithCCE("list as MutableIterable") { list as MutableIterable<*> }
    asFailsWithCCE("list as MutableCollection") { list as MutableCollection<*> }
    asFailsWithCCE("list as MutableList") { list as MutableList<*> }
    asSucceeds("mlist as MutableIterable") { mlist as MutableIterable<*> }
    asSucceeds("mlist as MutableCollection") { mlist as MutableCollection<*> }
    asSucceeds("mlist as MutableList") { mlist as MutableList<*> }

    val set = S() as Any
    val mset = MS()
    val hashSet = HashSet<String>()

    asFailsWithCCE("set as MutableIterable") { set as MutableIterable<*> }
    asFailsWithCCE("set as MutableCollection") { set as MutableCollection<*> }
    asFailsWithCCE("set as MutableSet") { set as MutableSet<*> }
    asSucceeds("mset as MutableIterable") { mset as MutableIterable<*> }
    asSucceeds("mset as MutableCollection") { mset as MutableCollection<*> }
    asSucceeds("mset as MutableSet") { mset as MutableSet<*> }
    asSucceeds("hashSet as MutableSet") { hashSet as MutableSet<*> }

    val map = M() as Any
    val mmap = MM()
    val hashMap = HashMap<String, String>()

    asFailsWithCCE("map as MutableMap") { map as MutableMap<*, *> }
    asSucceeds("mmap as MutableMap") { mmap as MutableMap<*, *> }

    val entry = ME() as Any
    val mentry = MME()

    asFailsWithCCE("entry as MutableMap.MutableEntry") { entry as MutableMap.MutableEntry<*, *> }
    asSucceeds("mentry as MutableMap.MutableEntry") { mentry as MutableMap.MutableEntry<*, *> }

    hashMap[""] = ""
    val hashMapEntry = hashMap.entries.first()

    asSucceeds("hashMapEntry as MutableMap.MutableEntry") { hashMapEntry as MutableMap.MutableEntry<*, *> }

    return "OK"
}
