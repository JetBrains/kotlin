import kotlin.*
import kotlin.collections.*
import kotlin.text.*

const val a = listOf(1, 2, 3).<!EVALUATED: `2`!>elementAtOrElse(1) { -1 }<!>
const val b = listOf(1, 2, 3).<!EVALUATED: `-1`!>elementAtOrElse(4) { -1 }<!>
const val c = uintArrayOf(1u, 2u, 3u, 4u).<!EVALUATED: `3`!>elementAtOrElse(2) { 0u }<!>
const val d = "abcd".<!EVALUATED: `c`!>elementAtOrElse(2) { '0' }<!>
const val e = "abcd".<!EVALUATED: `0`!>elementAtOrElse(4) { '0' }<!>
