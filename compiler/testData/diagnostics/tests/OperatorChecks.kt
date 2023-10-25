// FIR_IDENTICAL
// !DIAGNOSTICS: -EXTENSION_SHADOWED_BY_MEMBER

import kotlin.reflect.KProperty

interface Example {
    operator fun plus(o: Example): Example
    operator fun div(o: Example): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(o: Example, s: String = ""): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minus(vararg o: Example): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minus(): Example

    operator fun unaryPlus(): Example
    operator fun unaryMinus(): Example

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryPlus(s: String = ""): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryMinus(o: Example)

    operator fun inc(): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec(): Example?

    operator fun plusAssign(n: Int)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(n: Int): String
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun divAssign(n: Int, a: String = "")
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun modAssign(vararg n: Int)

    operator fun compareTo(other: Example): Int

    override operator fun equals(other: Any?): Boolean
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(a: String): Boolean

    operator fun contains(n: Int): Boolean
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(n: Int, s: String = ""): Boolean

    operator fun invoke()

    operator fun get(n: Int)
    operator fun get(n: Int, n2: Int)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get()

    operator fun set(n: Int, v: Int)
    operator fun set(n: Int, n2: Int, v: Int)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(v: Int)

    operator fun rangeTo(o: Int)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(o: Int, o2: Int)
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(vararg o: String)

    operator fun component1(): Int
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun component1(n: Int): Int
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun componentN(): Int

    operator fun iterator(): String
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun iterator(n: Int): String

    operator fun next(): String
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun next(n: Int): String

    operator fun hasNext(): Boolean
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(n: Int): String

    infix fun i1(n: Int)
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(n: Int, n2: Int)
    <!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(vararg n: Int)
}

class OkDelegates {
    operator fun getValue(thisRef: Any?, prop: KProperty<*>): String = ""
    operator fun setValue(thisRef: Any?, prop: KProperty<*>, s: String): String = ""
    operator fun setValue(thisRef: Any?, prop: Any, n: Int) {}
    operator fun setValue(thisRef: Any?, prop: Any?, s: String) {}
}

class DelegatesWithErrors {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun getValue(thisRef: Any?, prop: String): String = ""
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun setValue(thisRef: Any?, prop: String, value: String) {}

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun setValue(thisRef: Any?, prop: KProperty<*>, vararg n: Int) {}
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun setValue(thisRef: Any?, prop: KProperty<*>, f: Float = 0.0f) {}

    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun getValue(prop: KProperty<*>): String = ""
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun setValue(prop: KProperty<*>, value: String) {}
}

interface Example2 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(s: String): Example
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec()
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(vararg other: Example): Int
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(vararg n: Int): Boolean
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(): Int
}

interface Example3 {
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Example, s: String = ""): Int
    <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(n: Int)
}




operator fun Example.plus(o: Any): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.div(o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.plus(o: Example, s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minus(vararg o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.plus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.unaryPlus(s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.unaryMinus(o: Example) {}

operator fun Example.inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.plusAssign(n: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minusAssign(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.divAssign(n: Int, a: String = "") {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.modAssign(vararg n: Int) {}

operator fun Example.compareTo(other: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.equals(a: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.contains(n: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.contains(n: Int, s: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.invoke() {}

operator fun Example.get(n: Int) {}
operator fun Example.get(n: Int, n2: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.get() {}

operator fun Example.set(n: Int, v: Int) {}
operator fun Example.set(n: Int, n2: Int, v: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.set(v: Int) {}

operator fun Example.rangeTo(o: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.rangeTo(o: Int, o2: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.rangeTo(vararg o: String) {}

operator fun Example.component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.component1(n: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.iterator(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.next(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.hasNext(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

infix fun Example.i1(n: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.i1(n: Int, n2: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.i1(vararg n: Int) {}





<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(o: String): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun div(o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(o: Example, s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minus(vararg o: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryPlus(s: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryMinus(o: Example) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plusAssign(n: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun divAssign(n: Int, a: String = "") {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun modAssign(vararg n: Int) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(other: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(a: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(n: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(n: Int, s: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun invoke() {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get(n: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get(n: Int, n2: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get() {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(n: Int, v: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(n: Int, n2: Int, v: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(v: Int) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(o: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(o: Int, o2: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(vararg o: String) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun component1(n: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun iterator(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun next(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(n: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(n: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(n: Int, n2: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(vararg n: Int) {}
