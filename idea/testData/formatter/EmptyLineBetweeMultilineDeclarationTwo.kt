interface Some {
    fun f1()
    fun f2()
    fun f3()
    /* test */
    fun f4()
    /* test */ fun f5()
    /**
     * test
     * 2
     */
    fun f6()
    fun f7()
    @NotNull
    fun f8()
    fun f9()
}

interface Some2 {
    fun f1()
    fun f2()
    fun f3()
    @Ann
    fun f4()
    @Ann fun f5()
    @Ann(
        42
    )
    fun f6()
    fun f7()
    @NotNull
    fun f8()
    fun f9()
}

abstract class Abstract() {
    abstract fun f1()
    abstract fun f2()
    // test
    abstract fun f3()
    // test
    /* test */ abstract fun f4()
    abstract fun f5()
    abstract fun f6()
    /* test */ abstract fun f7()
}

abstract class Abstract() {
    abstract fun f1()
    abstract fun f2()
    @Ann
    abstract fun f3()
    @Ann
    @Ann abstract fun f4()
    abstract fun f5()
    abstract fun f6()
    @Ann abstract fun f7()
}

fun f1() {}
class C1 {}

// -----

fun f2() {}
class C2

// -----

// test
fun f3 = 1
class C3 {}

// -----

/*test*/
fun f4 = 2
class C4

// -----

@Ann
fun f3 = 1
class C3 {}

// -----

@Ann
fun f4 = 2
class C4

// -----

class C5 {}
fun f5() {}

// -----

class C6
fun f6() {}

// -----

class C7 {}
fun f7 = 1

// -----

class D {

}
class E
    : Some
class F

@Ann
fun some() = 1
class G
fun some() = 1

val a = 1
class H
val b = 1

// No lines
fun f1() {}
val p1 = 1
fun f2() {}

@Ann
fun f3() = 1
val p2 = 1
fun f4() = 1

fun f4() {}
val p3: Int
    get() = 1
fun f5() = 1

class OneLine {
    fun f1() {}

    val p1 = 1

    fun f2() {}

    fun f3() = 1

    val p2 = 1

    fun f4() = 1

    fun f4() {}

    val p3: Int
        get() = 1

    fun f5() = 1
}

class TwoLines {
    fun f1() {}


    val p1 = 1


    fun f2() {}


    fun f3() = 1


    val p2 = 1


    fun f4() = 1


    fun f4() {}


    val p3: Int
        get() = 1


    fun f5() = 1
}

// No lines between

fun f2() {}
fun f1() {}
fun f3() = 1
fun f4() {}
fun f5() {
}
fun f6() = 1
fun f7() = 8

// One line

fun f2() {}

fun f1() {}

fun f3() = 1

fun f4() {}

fun f5() {
}

fun f6() = 1

fun f7() = 8
// test
fun f8() = 42

// Two lines between
fun l1() {}


fun l2() {}


fun l3() = 1


fun l4() {}


fun l5() {
}


fun l6() = 1


fun l7() = 8

// test
val p1 by Some
val p2 = 1
val p3: Int get() = 3
val p4: Int
    get() { return 1 }
val p5: Int

class OneLine {
    val p1 by Some

    val p2 = 1

    val p3: Int get() = 3

    val p4: Int
        get() { return 1 }

    val p5: Int
}

class TwoLines {
    val p1 by Some


    val p2 = 1


    val p3: Int get() = 3


    val p4: Int
        get() { return 1 }


    val p5: Int
}

class Some(b: Boolean) {
    // Comment.
    constructor(b: Int) : this(b == 0)
    /**
     * test
     * 2
     */
    constructor(b: String) : this(b.isEmpty())
    constructor(b: Long) : this(b == 0L)
    @Ann
    constructor(b: Int) : this(b == 0)
    @Ann(
        42
    )
    constructor(b: String) : this(b.isEmpty())
    constructor(b: Long) : this(b == 0L)
}

enum class TestEnum {
    /** A comment for the item #1. */
    ITEM1,
    /** A comment for the item #2. */
    ITEM2,
    /** A comment for the item #3. */
    ITEM3
}

class Foo {
    @Inject
    lateinit var logger: Logger
    @Inject
    lateinit var userService: UserService
    @Inject
    override lateinit var configBridge: ConfigBridge
}

// SET_INT: BLANK_LINES_BEFORE_DECLARATION_WITH_COMMENT_OR_ANNOTATION_ON_SEPARATE_LINE = 2