import kotlin.*
import kotlin.collections.*
import kotlin.text.*

const val a = <!EVALUATED: `a`!>"abcs".first()<!>
const val b = <!EVALUATED: `0`!>UIntArray(3) { it.toUInt() }.first()<!>
const val c = <!EVALUATED: `1`!>listOf(1, "2", 3.0).first() as Int<!>
const val d = <!WAS_NOT_EVALUATED: `
Exception java.util.NoSuchElementException: List is empty.
	at CollectionsKt.kotlin.collections.first(Collections.kt:88)
	at FirstKt.<clinit>(first.kt:8)`!>listOf<Int>().first()<!>
