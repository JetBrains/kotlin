package abstract

class MyClass() {
    //properties

    <error>val a: Int</error>
    val a1: Int = 1
    <error>abstract</error> val a2: Int
    <error>abstract</error> val a3: Int = 1

    <error>var b: Int</error>                private set
    var b1: Int = 0;                         private set
    <error>abstract</error> var b2: Int      private set
    <error>abstract</error> var b3: Int = 0; private set

    <error>var c: Int</error>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    <error>abstract</error> var c2: Int      set(v: Int) { $c2 = v }
    <error>abstract</error> var c3: Int = 0; set(v: Int) { $c3 = v }

    val e: Int                               get() = a
    val e1: Int = <error>0</error>;          get() = a
    <error>abstract</error> val e2: Int      get() = a
    <error>abstract</error> val e3: Int = 0; get() = a

    //methods

    <error>fun f()</error>
    fun g() {}
    <error>abstract</error> fun h()
    <error><error>abstract</error></error> fun j() {}
}

abstract class MyAbstractClass() {
    //properties

    <error>val a: Int</error>
    val a1: Int = 1
    abstract val a2: Int
    abstract val a3: Int = <error>1</error>

    <error>var b: Int</error>                private set
    var b1: Int = 0;                         private set
    abstract var b2: Int      private set
    abstract var b3: Int = <error>0</error>; private set

    <error>var c: Int</error>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    abstract var c2: Int                     <error>set(v: Int) { $c2 = v }</error>
    abstract var c3: Int = <error>0</error>; <error>set(v: Int) { $c3 = v }</error>

    val e: Int                               get() = a
    val e1: Int = <error>0</error>;          get() = a
    abstract val e2: Int                     <error>get() = a</error>
    abstract val e3: Int = <error>0</error>; <error>get() = a</error>

    //methods

    <error>fun f()</error>
    fun g() {}
    abstract fun h()
    <error>abstract</error> fun j() {}
}

interface MyTrait {
    //properties

    val a: Int
    val a1: Int = <error>1</error>
    <warning>abstract</warning> val a2: Int
    <warning>abstract</warning> val a3: Int = <error>1</error>

    var b: Int                                                  private set
    var b1: Int = <error>0</error>;                             private set
    <warning>abstract</warning> var b2: Int                     private set
    <warning>abstract</warning> var b3: Int = <error>0</error>; private set

    <error>var c: Int</error>                                   set(v: Int) { $c = v }
    <error>var c1: Int</error> = <error>0</error>;              set(v: Int) { $c1 = v }
    <warning>abstract</warning> var c2: Int                     <error>set(v: Int) { $c2 = v }</error>
    <warning>abstract</warning> var c3: Int = <error>0</error>; <error>set(v: Int) { $c3 = v }</error>

    val e: Int                                                  get() = a
    val e1: Int = <error>0</error>;                             get() = a
    <warning>abstract</warning> val e2: Int                     <error>get() = a</error>
    <warning>abstract</warning> val e3: Int = <error>0</error>; <error>get() = a</error>

    //methods

    fun f()
    fun g() {}
    <warning>abstract</warning> fun h()
    <error>abstract</error> fun j() {}
}

enum class MyEnum() {
    ;
    //properties

    <error>val a: Int</error>
    val a1: Int = 1
    abstract val a2: Int
    abstract val a3: Int = <error>1</error>

    <error>var b: Int</error>                private set
    var b1: Int = 0;                         private set
    abstract var b2: Int      private set
    abstract var b3: Int = <error>0</error>; private set

    <error>var c: Int</error>                set(v: Int) { $c = v }
    var c1: Int = 0;                         set(v: Int) { $c1 = v }
    abstract var c2: Int                     <error>set(v: Int) { $c2 = v }</error>
    abstract var c3: Int = <error>0</error>; <error>set(v: Int) { $c3 = v }</error>

    val e: Int                               get() = a
    val e1: Int = <error>0</error>;          get() = a
    abstract val e2: Int                     <error>get() = a</error>
    abstract val e3: Int = <error>0</error>; <error>get() = a</error>

    //methods

    <error>fun f()</error>
    fun g() {}
    abstract fun h()
    <error>abstract</error> fun j() {}
}

<error>abstract</error> enum class MyAbstractEnum() {}

//properties

<error>val a: Int</error>
val a1: Int = 1
<error><error>abstract</error> val a2: Int</error>
<error>abstract</error> val a3: Int = 1

<error>var b: Int</error>                private set
var b1: Int = 0;                         private set
<error><error>abstract</error> var b2: Int</error>      private set
<error>abstract</error> var b3: Int = 0; private set

<error>var c: Int</error>                set(v: Int) { $c = v }
var c1: Int = 0;                         set(v: Int) { $c1 = v }
<error><error>abstract</error> var c2: Int</error>      set(v: Int) { $c2 = v }
<error>abstract</error> var c3: Int = 0; set(v: Int) { $c3 = v }

val e: Int                               get() = a
val e1: Int = <error>0</error>;          get() = a
<error>abstract</error> val e2: Int      get() = a
<error>abstract</error> val e3: Int = <error>0</error>; get() = a

//methods

<error>fun f()</error>
fun g() {}
<error>abstract</error> fun h()
<error>abstract</error> fun j() {}

//creating an instance
abstract class B1(
    val i: Int,
    val s: String
) {
}

class B2() : B1(1, "r") {}

abstract class B3(<warning>i</warning>: Int) {
}

fun foo(<warning>a</warning>: B3) {
    val <warning><warning>a</warning></warning> = <error>B3(1)</error>
    val <warning>b</warning> = <error>B1(2, "s")</error>
}