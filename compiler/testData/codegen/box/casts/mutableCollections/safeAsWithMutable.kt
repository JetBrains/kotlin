// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// WITH_RUNTIME

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

fun assert(condition: Boolean, message: () -> String) { if (!condition) throw AssertionError(message())}


inline fun safeAsReturnsNull(operation: String, cast: () -> Any?) {
    try {
        val x = cast()
        assert(x == null) { "$operation: should return null, got $x" }
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

inline fun safeAsReturnsNonNull(operation: String, cast: () -> Any?) {
    try {
        val x = cast()
        assert(x != null) { "$operation: should return non-null" }
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
}

fun box(): String {
    val itr = Itr() as Any
    val mitr = MItr()

    safeAsReturnsNull("itr as? MutableIterator") { itr as? MutableIterator<*> }
    safeAsReturnsNonNull("mitr as? MutableIterator") { mitr as? MutableIterator<*> }

    val litr = LItr() as Any
    val mlitr = MLItr()

    safeAsReturnsNull("litr as? MutableIterator") { litr as? MutableIterator<*> }
    safeAsReturnsNull("litr as? MutableListIterator") { litr as? MutableListIterator<*> }
    safeAsReturnsNonNull("mlitr as? MutableIterator") { mlitr as? MutableIterator<*> }
    safeAsReturnsNonNull("mlitr as? MutableListIterator") { mlitr as? MutableListIterator<*> }

    val it = It() as Any
    val mit = MIt()
    val arrayList = ArrayList<String>()

    safeAsReturnsNull("it as? MutableIterable") { it as? MutableIterable<*> }
    safeAsReturnsNonNull("mit as? MutableIterable") { mit as? MutableIterable<*> }
    safeAsReturnsNonNull("arrayList as? MutableIterable") { arrayList as? MutableIterable<*> }

    val coll = C() as Any
    val mcoll = MC()

    safeAsReturnsNull("coll as? MutableIterable") { coll as? MutableIterable<*> }
    safeAsReturnsNull("coll as? MutableCollection") { coll as? MutableCollection<*> }
    safeAsReturnsNonNull("mcoll as? MutableIterable") { mcoll as? MutableIterable<*> }
    safeAsReturnsNonNull("mcoll as? MutableCollection") { mcoll as? MutableCollection<*> }
    safeAsReturnsNonNull("arrayList as? MutableCollection") { arrayList as? MutableCollection<*> }

    val list = L() as Any
    val mlist = ML()

    safeAsReturnsNull("list as? MutableIterable") { list as? MutableIterable<*> }
    safeAsReturnsNull("list as? MutableCollection") { list as? MutableCollection<*> }
    safeAsReturnsNull("list as? MutableList") { list as? MutableList<*> }
    safeAsReturnsNonNull("mlist as? MutableIterable") { mlist as? MutableIterable<*> }
    safeAsReturnsNonNull("mlist as? MutableCollection") { mlist as? MutableCollection<*> }
    safeAsReturnsNonNull("mlist as? MutableList") { mlist as? MutableList<*> }

    val set = S() as Any
    val mset = MS()
    val hashSet = HashSet<String>()

    safeAsReturnsNull("set as? MutableIterable") { set as? MutableIterable<*> }
    safeAsReturnsNull("set as? MutableCollection") { set as? MutableCollection<*> }
    safeAsReturnsNull("set as? MutableSet") { set as? MutableSet<*> }
    safeAsReturnsNonNull("mset as? MutableIterable") { mset as? MutableIterable<*> }
    safeAsReturnsNonNull("mset as? MutableCollection") { mset as? MutableCollection<*> }
    safeAsReturnsNonNull("mset as? MutableSet") { mset as? MutableSet<*> }
    safeAsReturnsNonNull("hashSet as? MutableSet") { hashSet as? MutableSet<*> }

    val map = M() as Any
    val mmap = MM()
    val hashMap = HashMap<String, String>()

    safeAsReturnsNull("map as? MutableMap") { map as? MutableMap<*, *> }
    safeAsReturnsNonNull("mmap as? MutableMap") { mmap as? MutableMap<*, *> }
    safeAsReturnsNonNull("hashMap as? MutableMap") { hashMap as? MutableMap<*, *> }

    val entry = ME() as Any
    val mentry = MME()

    safeAsReturnsNull("entry as? MutableMap.MutableEntry") { entry as? MutableMap.MutableEntry<*, *> }
    safeAsReturnsNonNull("mentry as? MutableMap.MutableEntry") { mentry as? MutableMap.MutableEntry<*, *> }

    hashMap[""] = ""
    val hashMapEntry = hashMap.entries.first()

    safeAsReturnsNonNull("hashMapEntry as? MutableMap.MutableEntry") { hashMapEntry as? MutableMap.MutableEntry<*, *> }

    safeAsReturnsNull("null as? MutableIterator") { null as? MutableIterator<*> }
    safeAsReturnsNull("null as? MutableListIterator") { null as? MutableListIterator<*> }
    safeAsReturnsNull("null as? MutableIterable") { null as? MutableIterable<*> }
    safeAsReturnsNull("null as? MutableCollection") { null as? MutableCollection<*> }
    safeAsReturnsNull("null as? MutableList") { null as? MutableList<*> }
    safeAsReturnsNull("null as? MutableSet") { null as? MutableSet<*> }
    safeAsReturnsNull("null as? MutableMap") { null as? MutableMap<*, *> }
    safeAsReturnsNull("null as? MutableMap.MutableEntry") { null as? MutableMap.MutableEntry<*, *> }

    safeAsReturnsNull("mlist as? MutableSet") { mlist as? MutableSet<*> }
    safeAsReturnsNull("mlist as? MutableIterator") { mlist as? MutableIterator<*> }

    return "OK"
}
