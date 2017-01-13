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




operator fun Example.plus(<!UNUSED_PARAMETER!>o<!>: Any): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.div(<!UNUSED_PARAMETER!>o<!>: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.plus(<!UNUSED_PARAMETER!>o<!>: Example, <!UNUSED_PARAMETER!>s<!>: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minus(vararg <!UNUSED_PARAMETER!>o<!>: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.plus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
operator fun Example.unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.unaryPlus(<!UNUSED_PARAMETER!>s<!>: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.unaryMinus(<!UNUSED_PARAMETER!>o<!>: Example) {}

operator fun Example.inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.plusAssign(<!UNUSED_PARAMETER!>n<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.minusAssign(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.divAssign(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>a<!>: String = "") {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.modAssign(vararg <!UNUSED_PARAMETER!>n<!>: Int) {}

operator fun Example.compareTo(<!UNUSED_PARAMETER!>other<!>: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.equals(<!UNUSED_PARAMETER!>a<!>: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.contains(<!UNUSED_PARAMETER!>n<!>: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.contains(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>s<!>: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.invoke() {}

operator fun Example.get(<!UNUSED_PARAMETER!>n<!>: Int) {}
operator fun Example.get(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.get() {}

operator fun Example.set(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>v<!>: Int) {}
operator fun Example.set(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int, <!UNUSED_PARAMETER!>v<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.set(<!UNUSED_PARAMETER!>v<!>: Int) {}

operator fun Example.rangeTo(<!UNUSED_PARAMETER!>o<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.rangeTo(<!UNUSED_PARAMETER!>o<!>: Int, <!UNUSED_PARAMETER!>o2<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.rangeTo(vararg <!UNUSED_PARAMETER!>o<!>: String) {}

operator fun Example.component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.component1(<!UNUSED_PARAMETER!>n<!>: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.iterator(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.next(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

operator fun Example.hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun Example.hasNext(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

infix fun Example.i1(<!UNUSED_PARAMETER!>n<!>: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.i1(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun Example.i1(vararg <!UNUSED_PARAMETER!>n<!>: Int) {}





<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(<!UNUSED_PARAMETER!>o<!>: String): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun div(<!UNUSED_PARAMETER!>o<!>: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plus(<!UNUSED_PARAMETER!>o<!>: Example, <!UNUSED_PARAMETER!>s<!>: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minus(vararg <!UNUSED_PARAMETER!>o<!>: Example): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryPlus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryMinus(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryPlus(<!UNUSED_PARAMETER!>s<!>: String = ""): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun unaryMinus(<!UNUSED_PARAMETER!>o<!>: Example) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun inc(): Example {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun dec(): Example? {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun plusAssign(<!UNUSED_PARAMETER!>n<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun minusAssign(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun divAssign(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>a<!>: String = "") {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun modAssign(vararg <!UNUSED_PARAMETER!>n<!>: Int) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun compareTo(<!UNUSED_PARAMETER!>other<!>: Example): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun equals(<!UNUSED_PARAMETER!>a<!>: String): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(<!UNUSED_PARAMETER!>n<!>: Int): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun contains(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>s<!>: String = ""): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun invoke() {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get(<!UNUSED_PARAMETER!>n<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun get() {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>v<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int, <!UNUSED_PARAMETER!>v<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun set(<!UNUSED_PARAMETER!>v<!>: Int) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(<!UNUSED_PARAMETER!>o<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(<!UNUSED_PARAMETER!>o<!>: Int, <!UNUSED_PARAMETER!>o2<!>: Int) {}
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun rangeTo(vararg <!UNUSED_PARAMETER!>o<!>: String) {}

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun component1(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun component1(<!UNUSED_PARAMETER!>n<!>: Int): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun componentN(): Int {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun iterator(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun iterator(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun next(): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun next(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(): Boolean {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>
<!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun hasNext(<!UNUSED_PARAMETER!>n<!>: Int): String {<!NO_RETURN_IN_FUNCTION_WITH_BLOCK_BODY!>}<!>

<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(<!UNUSED_PARAMETER!>n<!>: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(<!UNUSED_PARAMETER!>n<!>: Int, <!UNUSED_PARAMETER!>n2<!>: Int) {}
<!INAPPLICABLE_INFIX_MODIFIER!>infix<!> fun i1(vararg <!UNUSED_PARAMETER!>n<!>: Int) {}
