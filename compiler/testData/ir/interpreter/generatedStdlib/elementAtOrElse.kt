import kotlin.*
import kotlin.collections.*
import kotlin.text.*

const val a = <!EVALUATED: `2`!>listOf(1, 2, 3).elementAtOrElse(1) { -1 }<!>
const val b = <!EVALUATED: `-1`!>listOf(1, 2, 3).elementAtOrElse(4) { -1 }<!>
const val c = <!EVALUATED: `3`!>uintArrayOf(1u, 2u, 3u, 4u).elementAtOrElse(2) { 0u }<!>
const val d = <!EVALUATED: `c`!>"abcd".elementAtOrElse(2) { '0' }<!>
const val e = <!EVALUATED: `0`!>"abcd".elementAtOrElse(4) { '0' }<!>
