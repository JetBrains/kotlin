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

fun box(): String {
    val itr = Itr() as Any
    val mitr = MItr()

    assert(itr !is MutableIterator<*>) { "Itr should satisfy '!is MutableIterator'" }
    assert(mitr is MutableIterator<*>) { "MItr should satisfy 'is MutableIterator'" }

    val litr = LItr() as Any
    val mlitr = MLItr()

    assert(litr !is MutableIterator<*>) { "LItr should satisfy '!is MutableIterator'" }
    assert(litr !is MutableListIterator<*>) { "LItr should satisfy '!is MutableListIterator'" }
    assert(mlitr is MutableListIterator<*>) { "MLItr should satisfy 'is MutableListIterator'" }

    val it = It() as Any
    val mit = MIt()
    val arrayList = ArrayList<String>()

    assert(it !is MutableIterable<*>) { "It should satisfy '!is MutableIterable'" }
    assert(mit is MutableIterable<*>) { "MIt should satisfy 'is MutableIterable'" }
    assert(arrayList is MutableIterable<*>) { "ArrayList should satisfy 'is MutableIterable'" }

    val coll = C() as Any
    val mcoll = MC()

    assert(coll !is MutableCollection<*>) { "C should satisfy '!is MutableCollection'" }
    assert(coll !is MutableIterable<*>) { "C should satisfy '!is MutableIterable'" }
    assert(mcoll is MutableCollection<*>) { "MC should satisfy 'is MutableCollection'" }
    assert(mcoll is MutableIterable<*>) { "MC should satisfy 'is MutableIterable'" }
    assert(arrayList is MutableCollection<*>) { "ArrayList should satisfy 'is MutableCollection'" }

    val list = L() as Any
    val mlist = ML()

    assert(list !is MutableList<*>) { "L should satisfy '!is MutableList'" }
    assert(list !is MutableCollection<*>) { "L should satisfy '!is MutableCollection'" }
    assert(list !is MutableIterable<*>) { "L should satisfy '!is MutableIterable'" }
    assert(mlist is MutableList<*>) { "ML should satisfy 'is MutableList'" }
    assert(mlist is MutableCollection<*>) { "ML should satisfy 'is MutableCollection'" }
    assert(mlist is MutableIterable<*>) { "ML should satisfy 'is MutableIterable'" }
    assert(arrayList is MutableList<*>) { "ArrayList should satisfy 'is MutableList'" }

    val set = S() as Any
    val mset = MS()
    val hashSet = HashSet<String>()

    assert(set !is MutableSet<*>) { "S should satisfy '!is MutableSet'" }
    assert(set !is MutableCollection<*>) { "S should satisfy '!is MutableCollection'" }
    assert(set !is MutableIterable<*>) { "S should satisfy '!is MutableIterable'" }
    assert(mset is MutableSet<*>) { "MS should satisfy 'is MutableSet'" }
    assert(mset is MutableCollection<*>) { "MS should satisfy 'is MutableCollection'" }
    assert(mset is MutableIterable<*>) { "MS should satisfy 'is MutableIterable'" }
    assert(hashSet is MutableSet<*>) { "HashSet should satisfy 'is MutableSet'" }
    assert(hashSet is MutableCollection<*>) { "HashSet should satisfy 'is MutableCollection'" }
    assert(hashSet is MutableIterable<*>) { "HashSet should satisfy 'is MutableIterable'" }

    val map = M() as Any
    val mmap = MM()
    val hashMap = HashMap<String, String>()

    assert(map !is MutableMap<*, *>) { "M should satisfy '!is MutableMap'" }
    assert(mmap is MutableMap<*, *>) { "MM should satisfy 'is MutableMap'"}
    assert(hashMap is MutableMap<*, *>) { "HashMap should satisfy 'is MutableMap'" }

    val entry = ME() as Any
    val mentry = MME()

    hashMap[""] = ""
    val hashMapEntry = hashMap.entries.first()

    assert(entry !is MutableMap.MutableEntry<*, *>) { "ME should satisfy '!is MutableMap.MutableEntry'"}
    assert(mentry is MutableMap.MutableEntry<*, *>) { "MME should satisfy 'is MutableMap.MutableEntry'"}
    assert(hashMapEntry is MutableMap.MutableEntry<*, *>) { "HashMap.Entry should satisfy 'is MutableMap.MutableEntry'"}

    assert((mlist as Any) !is MutableSet<*>) { "ML !is MutableSet" }
    assert((mlist as Any) !is MutableIterator<*>) { "ML !is MutableIterator" }

    return "OK"
}
