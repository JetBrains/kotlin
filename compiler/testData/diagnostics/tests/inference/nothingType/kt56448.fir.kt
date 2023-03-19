// SKIP_TXT
// WITH_STDLIB
// ISSUE: KT-56448
// FILE: SomeJavaClass.java

public class SomeJavaClass {
    public static String getString() {
        return "";
    }
}

// FILE: main.kt
import kotlin.reflect.KProperty1

interface XdEntity
class XdIssue : XdEntity {
    val isRemoved: Boolean = true
    var votes: Int = 0
}

fun getNullableIssue(): XdIssue? = null

fun <T : XdEntity> CharSequence.toXd(): T = null!!

fun XdIssue.duplicatesRootSearch(): XdIssue = this
fun <T : XdEntity, R : Comparable<*>?> T.getOldValue(property: KProperty1<T, R>): R? = null

internal fun updateVotesForDuplicates(issue: XdIssue) {
    val toRecount = HashSet<XdIssue>()
    var oldDup = getNullableIssue()
    if (oldDup != null) {
        oldDup = try {
            <!DEBUG_INFO_EXPRESSION_TYPE("XdIssue")!>SomeJavaClass.getString().toXd()<!>
        } catch (_: Exception) {
            null
        }
    }
    val newDup = getNullableIssue()

    if (oldDup != null && !oldDup.isRemoved) {
        toRecount.add(oldDup.duplicatesRootSearch())
    }
}
