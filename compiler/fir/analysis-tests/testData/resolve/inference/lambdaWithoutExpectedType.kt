// WITH_STDLIB

interface XdEntity

interface XdEntityType<out T : XdEntity>
interface XdNaturalEntityType<T : XdEntity> : XdEntityType<T>

class XdProject(var name: String) : XdEntity {
    companion object : XdNaturalEntityType<XdProject> {}
}

interface XdSearchingNode
interface XdQuery<out T : XdEntity>

fun <T : XdEntity> XdQuery<T>.firstOrNull(): T? = null

object FilteringContext {
    infix fun <T : Comparable<T>> T?.eq(value: T?): XdSearchingNode = null!!
}

fun <T : XdEntity> XdEntityType<T>.filter(clause: FilteringContext.(T) -> XdSearchingNode): XdQuery<T> {
    return null!!
}

interface XdIssue
interface XdIssueFolder

fun test() {
    val array = arrayOf<Any>(
        "Project",
        { XdProject.filter { it.name eq "foo" }.firstOrNull() },
        { _: XdIssue, _: XdIssueFolder -> }
    )
}
