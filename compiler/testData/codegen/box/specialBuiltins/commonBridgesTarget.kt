// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: NATIVE

open class Base<Target : DatabaseEntity>() : HashSet<Target>() {
    override fun remove(element: Target): Boolean {
        return true
    }
}

class Derived : Base<Issue>() {
    // common "synthetic bridge override fun remove(element: DatabaseEntity): Boolean" should call
    // `INVOKEVIRTUAL remove(Issue)`
    // instead of `INVOKEVIRTUAL remove(OBJECT)`
    override fun remove(element: Issue): Boolean {
        return super.remove(element)
    }
}

open class DatabaseEntity
class Issue: DatabaseEntity()

fun box(): String {
    val sprintIssues = Derived()
    if (!sprintIssues.remove(Issue())) return "Fail"

    return "OK"
}
