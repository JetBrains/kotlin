class OutPair<out X, out Y>
class In<in Z>

interface A {
    fun foo1(): OutPair<String, Int>
    fun foo2(): OutPair<CharSequence, Int>
    fun foo3(): OutPair<OutPair<CharSequence, Number>, Number>

    fun foo4(): In<String>
    fun foo5(): In<Any>

    val prop1: OutPair<String, Int>
    val prop2: OutPair<CharSequence, Int>
}

// method: A::foo1
// generic signature: ()LOutPair<Ljava/lang/String;Ljava/lang/Integer;>;

// method: A::foo2
// generic signature: ()LOutPair<Ljava/lang/CharSequence;Ljava/lang/Integer;>;

// method: A::foo3
// generic signature: ()LOutPair<LOutPair<Ljava/lang/CharSequence;Ljava/lang/Number;>;Ljava/lang/Number;>;

// method: A::foo4
// generic signature: ()LIn<Ljava/lang/String;>;

// method: A::foo5
// generic signature: ()LIn<Ljava/lang/Object;>;

// method: A::getProp1
// generic signature: ()LOutPair<Ljava/lang/String;Ljava/lang/Integer;>;

// method: A::getProp2
// generic signature: ()LOutPair<Ljava/lang/CharSequence;Ljava/lang/Integer;>;

abstract class B : A {
    override fun foo2(): OutPair<CharSequence, Int> = null!!
    override fun foo3(): OutPair<OutPair<String, Int>, Int> = null!!

    override val prop2: OutPair<String, Int> = null!!
}

// method: B::foo2
// generic signature: ()LOutPair<Ljava/lang/CharSequence;Ljava/lang/Integer;>;

// method: B::foo3
// generic signature: ()LOutPair<LOutPair<Ljava/lang/String;Ljava/lang/Integer;>;Ljava/lang/Integer;>;

// method: B::getProp2
// generic signature: ()LOutPair<Ljava/lang/String;Ljava/lang/Integer;>;