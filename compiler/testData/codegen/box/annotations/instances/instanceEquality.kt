// WITH_STDLIB
// IGNORE_BACKEND_K1: ANY

annotation class A1
annotation class A2

fun interface I {
    fun run(): A1
}

class E {
    fun insideClass(): A1 = A1()
    fun insideLambda(): A1 = run { A1() }
    fun insideSAM(): A1 = I { A1() }.run()
}

class G {
    fun insideClassAgain(): A1 = A1()
}

fun outsideClass(): A2 = A2()

fun box(): String {
    val e = E()

    val aFromClass = e.insideClass()
    val aFromLambda = e.insideLambda()
    val aFromSam = e.insideSAM()
    val aFromAnother = G().insideClassAgain()
    val all = listOf(aFromClass, aFromLambda, aFromSam, aFromAnother)


    for (x in all) for (y in all) {
        if (x != y) return "Fail1"
    }

    val h = all.first().hashCode()
    if (all.any { it.hashCode() != h }) return "Fail2"

    val b = outsideClass()
    if (all.first() == b) return "Fail3"

    val set: MutableSet<Any> = hashSetOf(all.first() as Any)
    if (set.contains(b as Any)) return "Fail4"
    set.add(b)
    if (set.size != 2) return "Fail5"

    val map: MutableMap<Any, String> = hashMapOf(all.first() as Any to "ok")
    if (map[b] != null) return "Fail6"

    val ts = all.first().toString()
    if (ts.isEmpty() || !ts.contains("A1")) {
        return "Fail7"
    }

    return "OK"
}