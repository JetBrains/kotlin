// IGNORE_FE10
// KT-64503

// MODULE: unrelatedLibrary
// MODULE_KIND: LibraryBinary
// FILE: Unrelated.kt
package library.api

interface Unrelated

internal interface InternalUnrelated

private interface PrivateUnrelated

fun fooUnrelated(): String = "fooUnrelated"

internal fun internalFooUnrelated(): String = "internalFooUnrelated"

private fun privateFooUnrelated(): String = "privateFooUnrelated"

fun Unrelated.barUnrelated(): Unrelated = this

internal fun InternalUnrelated.internalBarUnrelated(): InternalUnrelated = this

private fun PrivateUnrelated.privateBarUnrelated(): PrivateUnrelated = this

// MODULE: library
// MODULE_KIND: LibraryBinary
// FILE: I.kt
package library.api

interface I

internal interface InternalI

private interface PrivateI

fun fooApi1(): String = "fooApi1"

internal fun internalFooApi1(): String = "internalFooApi1"

private fun privateFooApi1(): String = "privateFooApi1"

fun I.barApi1(): I = this

internal fun InternalI.internalBarApi1(): InternalI = this

private fun PrivateI.privateBarApi1(): PrivateI = this

// FILE: C1.kt
package library.api

class C1 : I

internal class InternalC1 : InternalI

private class PrivateC1 : I

fun fooApi2(): String = "fooApi2"

internal fun internalFooApi2(): String = "internalFooApi2"

private fun privateFooApi2(): String = "privateFooApi2"

fun C1.barApi2(): I = this

internal fun InternalC1.internalBarApi2(): InternalI = this

private fun PrivateC1.privateBarApi2(): I = this

// FILE: C2.kt
package library.impl

import library.api.*

class C2 : I

internal class InternalC2 : InternalI

private class PrivateC2 : I

fun fooImpl(): String = "fooImpl"

internal fun internalFooImpl(): String = "internalFooImpl"

private fun privateFooImpl(): String = "privateFooImpl"

fun C2.barImpl(): I = this

internal fun InternalC2.internalBarImpl(): InternalI = this

private fun PrivateC2.privateBarImpl(): I = this

// FILE: O.kt
package library

import library.api.*
import library.impl.*

object O

internal object InternalO

private object PrivateO

fun fooBase(): String = "fooBase"

internal fun internalFooBase(): String = "internalFooBase"

private fun privateFooBase(): String = "privateFooBase"

fun O.barBase(): I = C2()

internal fun InternalO.internalBarBase(): InternalI = InternalC2()

private fun PrivateO.privateBarBase(): I = C2()

// MODULE: main(library)
// FILE: extension.kt
package library.api

interface FakeI : I

internal interface InternalFakeI : I

private interface PrivateFakeI : I

fun fooApi3(): String = "fooApi3"

internal fun internalFooApi3(): String = "internalFooApi3"

private fun privateFooApi3(): String = "privateFooApi3"

fun FakeI.barApi3(): I = this

internal fun InternalFakeI.internalBarApi3(): I = this

private fun PrivateFakeI.privateBarApi3(): I = this

// FILE: main.kt
package test

import library.api.I

class TestClass : I

internal class InternalTestClass : I

private class PrivateTestClass : I

fun fooTest(): String = "fooTest"

internal fun internalFooTest(): String = "internalFooTest"

private fun privateFooTest(): String = "privateFooTest"

fun TestClass.barTest(): I = this

internal fun InternalTestClass.internalBarTest(): I = this

private fun PrivateTestClass.privateBarTest(): I = this

// package: library.api
