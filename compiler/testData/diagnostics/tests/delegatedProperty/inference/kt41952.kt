// FIR_IDENTICAL
// ISSUE: KT-41952

import kotlin.reflect.KClass
import kotlin.reflect.KProperty

class Issue {
    val strings by bidir_collection(String::class) {
        takeIssue(it) // Issue? instead of Issue
    }
}

fun takeIssue(issue: Issue) {}

fun <Self : Any, Target : Any> Self.bidir_collection(targetType: KClass<out Target>, f: (Self) -> Unit): Delegate<Self, Collection<Target>> = null!!

class Delegate<R, T> {
    operator fun getValue(thisRef: R, property: KProperty<*>): T {
        return null!!
    }

    operator fun setValue(thisRef: R, property: KProperty<*>, value: T) {}
}
