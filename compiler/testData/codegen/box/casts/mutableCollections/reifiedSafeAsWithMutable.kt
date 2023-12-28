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

inline fun <reified T> reifiedSafeAsReturnsNonNull(x: Any?, operation: String) {
    val y = try {
        x as? T
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
    if (y == null) {
        throw AssertionError("$operation: should return non-null, got null")
    }
}

inline fun <reified T> reifiedSafeAsReturnsNull(x: Any?, operation: String) {
    val y = try {
        x as? T
    }
    catch (e: Throwable) {
        throw AssertionError("$operation: should not throw exceptions, got $e")
    }
    if (y != null) {
        throw AssertionError("$operation: should return null, got $y")
    }
}

fun box(): String {
    val itr = Itr() as Any
    val mitr = MItr()

    reifiedSafeAsReturnsNull<MutableIterator<*>>(itr, "reifiedSafeAs<MutableIterator<*>>(itr)")
    reifiedSafeAsReturnsNonNull<MutableIterator<*>>(mitr, "reifiedSafeAs<MutableIterator<*>>(mitr)")

    val litr = LItr() as Any
    val mlitr = MLItr()

    reifiedSafeAsReturnsNull<MutableIterator<*>>(litr, "reifiedSafeAs<MutableIterator<*>>(litr)")
    reifiedSafeAsReturnsNull<MutableListIterator<*>>(litr, "reifiedSafeAs<MutableListIterator<*>>(litr)")
    reifiedSafeAsReturnsNonNull<MutableListIterator<*>>(mlitr, "reifiedSafeAs<MutableListIterator<*>>(mlitr)")

    val it = It() as Any
    val mit = MIt()
    val arrayList = ArrayList<String>()

    reifiedSafeAsReturnsNull<MutableIterable<*>>(it, "reifiedSafeAs<MutableIterable<*>>(it)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(mit, "reifiedSafeAs<MutableIterable<*>>(mit)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(arrayList, "reifiedSafeAs<MutableIterable<*>>(arrayList)")

    val coll = C() as Any
    val mcoll = MC()

    reifiedSafeAsReturnsNull<MutableCollection<*>>(coll, "reifiedSafeAs<MutableCollection<*>>(coll)")
    reifiedSafeAsReturnsNull<MutableIterable<*>>(coll, "reifiedSafeAs<MutableIterable<*>>(coll)")
    reifiedSafeAsReturnsNonNull<MutableCollection<*>>(mcoll, "reifiedSafeAs<MutableCollection<*>>(mcoll)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(mcoll, "reifiedSafeAs<MutableIterable<*>>(mcoll)")
    reifiedSafeAsReturnsNonNull<MutableCollection<*>>(arrayList, "reifiedSafeAs<MutableCollection<*>>(arrayList)")

    val list = L() as Any
    val mlist = ML()

    reifiedSafeAsReturnsNull<MutableList<*>>(list, "reifiedSafeAs<MutableList<*>>(list)")
    reifiedSafeAsReturnsNull<MutableCollection<*>>(list, "reifiedSafeAs<MutableCollection<*>>(list)")
    reifiedSafeAsReturnsNull<MutableIterable<*>>(list, "reifiedSafeAs<MutableIterable<*>>(list)")
    reifiedSafeAsReturnsNonNull<MutableList<*>>(mlist, "reifiedSafeAs<MutableList<*>>(mlist)")
    reifiedSafeAsReturnsNonNull<MutableCollection<*>>(mlist, "reifiedSafeAs<MutableCollection<*>>(mlist)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(mlist, "reifiedSafeAs<MutableIterable<*>>(mlist)")
    reifiedSafeAsReturnsNonNull<MutableList<*>>(arrayList, "reifiedSafeAs<MutableList<*>>(arrayList)")

    val set = S() as Any
    val mset = MS()
    val hashSet = HashSet<String>()

    reifiedSafeAsReturnsNull<MutableSet<*>>(set, "reifiedSafeAs<MutableSet<*>>(set)")
    reifiedSafeAsReturnsNull<MutableCollection<*>>(set, "reifiedSafeAs<MutableCollection<*>>(set)")
    reifiedSafeAsReturnsNull<MutableIterable<*>>(set, "reifiedSafeAs<MutableIterable<*>>(set)")
    reifiedSafeAsReturnsNonNull<MutableSet<*>>(mset, "reifiedSafeAs<MutableSet<*>>(mset)")
    reifiedSafeAsReturnsNonNull<MutableCollection<*>>(mset, "reifiedSafeAs<MutableCollection<*>>(mset)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(mset, "reifiedSafeAs<MutableIterable<*>>(mset)")
    reifiedSafeAsReturnsNonNull<MutableSet<*>>(hashSet, "reifiedSafeAs<MutableSet<*>>(hashSet)")
    reifiedSafeAsReturnsNonNull<MutableCollection<*>>(hashSet, "reifiedSafeAs<MutableCollection<*>>(hashSet)")
    reifiedSafeAsReturnsNonNull<MutableIterable<*>>(hashSet, "reifiedSafeAs<MutableIterable<*>>(hashSet)")

    val map = M() as Any
    val mmap = MM()
    val hashMap = HashMap<String, String>()

    reifiedSafeAsReturnsNull<MutableMap<*, *>>(map, "reifiedSafeAs<MutableMap<*, *>>(map)")
    reifiedSafeAsReturnsNonNull<MutableMap<*, *>>(mmap, "reifiedSafeAs<MutableMap<*, *>>(mmap)")
    reifiedSafeAsReturnsNonNull<MutableMap<*, *>>(hashMap, "reifiedSafeAs<MutableMap<*, *>>(hashMap)")

    val entry = ME() as Any
    val mentry = MME()

    hashMap[""] = ""
    val hashMapEntry = hashMap.entries.first()

    reifiedSafeAsReturnsNull<MutableMap.MutableEntry<*, *>>(entry, "reifiedSafeAs<MutableMap.MutableEntry<*, *>>(entry)")
    reifiedSafeAsReturnsNonNull<MutableMap.MutableEntry<*, *>>(mentry, "reifiedSafeAs<MutableMap.MutableEntry<*, *>>(mentry)")
    reifiedSafeAsReturnsNonNull<MutableMap.MutableEntry<*, *>>(hashMapEntry, "reifiedSafeAs<MutableMap.MutableEntry<*, *>>(hashMapEntry)")

    reifiedSafeAsReturnsNull<MutableIterator<*>>(null, "reifiedSafeAs<MutableIterator<*>>(null)")
    reifiedSafeAsReturnsNull<MutableListIterator<*>>(null, "reifiedSafeAs<MutableListIterator<*>>(null)")
    reifiedSafeAsReturnsNull<MutableIterable<*>>(null, "reifiedSafeAs<MutableIterable<*>>(null)")
    reifiedSafeAsReturnsNull<MutableCollection<*>>(null, "reifiedSafeAs<MutableCollection<*>>(null)")
    reifiedSafeAsReturnsNull<MutableList<*>>(null, "reifiedSafeAs<MutableList<*>>(null)")
    reifiedSafeAsReturnsNull<MutableSet<*>>(null, "reifiedSafeAs<MutableSet<*>>(null)")
    reifiedSafeAsReturnsNull<MutableMap<*, *>>(null, "reifiedSafeAs<MutableMap<*, *>>(null)")
    reifiedSafeAsReturnsNull<MutableMap.MutableEntry<*, *>>(null, "reifiedSafeAs<MutableMap.MutableEntry<*, *>>(null)")

    return "OK"
}
