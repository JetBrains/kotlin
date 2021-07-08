// !DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER

import kotlin.reflect.KProperty

interface Example {
    operator fun plus(o: Example): Example
    operator fun div(o: Example): Example
    operator fun plus(o: Example, s: String = ""): Example
    operator fun minus(vararg o: Example): Example
    operator fun plus(): Example
    operator fun minus(): Example

    operator fun unaryPlus(): Example
    operator fun unaryMinus(): Example

    operator fun unaryPlus(s: String = ""): Example
    operator fun unaryMinus(o: Example)

    operator fun inc(): Example
    operator fun dec(): Example?

    operator fun plusAssign(n: Int)
    operator fun minusAssign(n: Int): String
    operator fun divAssign(n: Int, a: String = "")
    operator fun modAssign(vararg n: Int)

    operator fun compareTo(other: Example): Int

    override operator fun equals(other: Any?): Boolean
    operator fun equals(a: String): Boolean

    operator fun contains(n: Int): Boolean
    operator fun contains(n: Int, s: String = ""): Boolean

    operator fun invoke()

    operator fun get(n: Int)
    operator fun get(n: Int, n2: Int)
    operator fun get()

    operator fun set(n: Int, v: Int)
    operator fun set(n: Int, n2: Int, v: Int)
    operator fun set(v: Int)

    operator fun rangeTo(o: Int)
    operator fun rangeTo(o: Int, o2: Int)
    operator fun rangeTo(vararg o: String)

    operator fun component1(): Int
    operator fun component1(n: Int): Int
    operator fun componentN(): Int

    operator fun iterator(): String
    operator fun iterator(n: Int): String

    operator fun next(): String
    operator fun next(n: Int): String

    operator fun hasNext(): Boolean
    operator fun hasNext(n: Int): String

    infix fun i1(n: Int)
    <!INAPPLICABLE_INFIX_MODIFIER!>infix fun i1(n: Int, n2: Int)<!>
    infix fun i1(vararg n: Int)
}

class OkDelegates {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = ""
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, s: String): String = ""
    operator fun setValue(thisRef: Any?, prop: Any, n: Int) {}
    operator fun setValue(thisRef: Any?, prop: Any?, s: String) {}
}

class DelegatesWithErrors {
    operator fun getValue(thisRef: Any?, prop: String): String = ""
    operator fun setValue(thisRef: Any?, prop: String, value: String) {}

    operator fun setValue(thisRef: Any?, prop: KProperty<*>, vararg n: Int) {}
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, f: Float = 0.0f) {}

    operator fun getValue(prop: KProperty<*>): String = ""
    operator fun setValue(prop: KProperty<*>, value: String) {}
}

interface Example2 {
    operator fun inc(s: String): Example
    operator fun dec()
    operator fun compareTo(vararg other: Example): Int
    operator fun contains(vararg n: Int): Boolean
    operator fun hasNext(): Int
}

interface Example3 {
    operator fun compareTo(other: Example, s: String = ""): Int
    operator fun contains(n: Int)
}




operator fun Example.plus(o: Any): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.div(o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.plus(o: Example, s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.minus(vararg o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.plus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.minus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.unaryPlus(s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.unaryMinus(o: Example) {}

operator fun Example.inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.plusAssign(n: Int) {}
operator fun Example.minusAssign(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.divAssign(n: Int, a: String = "") {}
operator fun Example.modAssign(vararg n: Int) {}

operator fun Example.compareTo(other: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.equals(a: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.contains(n: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.contains(n: Int, s: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.invoke() {}

operator fun Example.get(n: Int) {}
operator fun Example.get(n: Int, n2: Int) {}
operator fun Example.get() {}

operator fun Example.set(n: Int, v: Int) {}
operator fun Example.set(n: Int, n2: Int, v: Int) {}
operator fun Example.set(v: Int) {}

operator fun Example.rangeTo(o: Int) {}
operator fun Example.rangeTo(o: Int, o2: Int) {}
operator fun Example.rangeTo(vararg o: String) {}

operator fun Example.component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.component1(n: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.iterator(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.next(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.hasNext(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

infix fun Example.i1(n: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix fun Example.i1(n: Int, n2: Int) {}<!>
infix fun Example.i1(vararg n: Int) {}





operator fun plus(o: String): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun div(o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun plus(o: Example, s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun minus(vararg o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun unaryPlus(s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun unaryMinus(o: Example) {}

operator fun inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun plusAssign(n: Int) {}
operator fun minusAssign(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun divAssign(n: Int, a: String = "") {}
operator fun modAssign(vararg n: Int) {}

operator fun compareTo(other: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun equals(a: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun contains(n: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun contains(n: Int, s: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun invoke() {}

operator fun get(n: Int) {}
operator fun get(n: Int, n2: Int) {}
operator fun get() {}

operator fun set(n: Int, v: Int) {}
operator fun set(n: Int, n2: Int, v: Int) {}
operator fun set(v: Int) {}

operator fun rangeTo(o: Int) {}
operator fun rangeTo(o: Int, o2: Int) {}
operator fun rangeTo(vararg o: String) {}

operator fun component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun component1(n: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun iterator(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun next(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun hasNext(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_INFIX_MODIFIER!>infix fun i1(n: Int) {}<!>
<!INAPPLICABLE_INFIX_MODIFIER!>infix fun i1(n: Int, n2: Int) {}<!>
<!INAPPLICABLE_INFIX_MODIFIER!>infix fun i1(vararg n: Int) {}<!>
