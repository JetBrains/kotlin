import kotlin.*
import kotlin.collections.*
import kotlin.text.*

const val a = "abcs".<!EVALUATED: `a`!>first()<!>
const val b = UIntArray(3) { it.toUInt() }.<!EVALUATED: `0`!>first()<!>
const val c = <!EVALUATED: `1`!>listOf(1, "2", 3.0).first() as Int<!>
const val d = listOf<Int>().<!WAS_NOT_EVALUATED: `
Exception java.util.NoSuchElementException: List is empty.
	at CollectionsKt.kotlin.collections.first(Collections.kt:88)
	at FirstKt.<clinit>(first.kt:8)`!>first()<!>
