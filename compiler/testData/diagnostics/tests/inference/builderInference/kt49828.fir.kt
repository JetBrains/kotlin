// WITH_STDLIB
// !DIAGNOSTICS: -OPT_IN_USAGE_ERROR
// For FIR, see: KT-50704

@JvmName("foo1")
fun foo(x: Inv<String>) {}
fun foo(x: Inv<Int>) {}

@JvmName("foo21")
fun Inv<String>.foo2() {}
fun Inv<Int>.foo2() {}

@JvmName("bar1")
fun String.bar() {}
fun Int.bar() {}

class Inv<K>(x: K)

fun foo0(x: String, y: Float, z: String = "") {}
fun foo0(x: String, y: Float, z: Int = 1) {}

fun foo00(x: String, y: Number) {}
fun foo00(x: CharSequence, y: Float) {}

fun foo000(x: String, y: Number, z: String) {}
fun foo000(x: CharSequence, y: Float, z: Int) {}

fun foo0000(x: String, y: Number, z: String) {}
fun foo0000(x: Int, y: Float, z: Int) {}

fun foo0001(x: List<Int>, y: Number, z: String) {}
fun foo0001(x: String, y: Float, z: Int) {}

fun foo0002(x: Int, y: Number, z: String) {}
fun foo0002(x: String, y: Float, z: Int) {}

fun Int.foo0003(y: Number, z: String) {}
fun String.foo0003(y: Float, z: Int) {}

@JvmName("foo111")
fun foo11(x: MutableSet<MutableMap.MutableEntry<String, Int>>) {}
@JvmName("foo112")
fun foo11(x: MutableSet<MutableMap.MutableEntry<Int, String>>) {}
fun foo11(x: MutableSet<MutableMap.MutableEntry<Int, Int>>) {}

fun main() {
    val list1 = buildList {
        add("one")

        val secondParameter = get(1)
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(secondParameter) // ERROR: [OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity. All these functions match.
    }
    val list2 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>println<!>(get(1)) // ERROR: [OVERLOAD_RESOLUTION_AMBIGUITY] Overload resolution ambiguity. All these functions match.
    }
    val list3 = buildList {
        add("one")

        val secondParameter = Inv(get(1))
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(secondParameter)
    }
    val list4 = buildList {
        add("one")

        val secondParameter = get(1)
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(Inv(secondParameter))
    }
    val list5 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo<!>(Inv(get(1)))
    }
    val list6 = buildList {
        add("one")

        get(0).<!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
    }
    val list7 = buildList {
        add("one")

        with (get(0)) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
        }
    }
    val list71 = buildList {
        add("one")

        with (get(0)) l1@ {
            with (listOf(1)) {
                <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
            }
        }
    }
    val list711 = buildList {
        add("one")

        with (get(0)) {
            with (listOf(1)) {
                <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
            }
        }
    }
    val list8 = buildList {
        add("one")

        Inv(get(0)).<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>()
    }
    val list9 = buildList {
        add("one")

        with (get(0)) {
            with (Inv(this)) {
                <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo2<!>()
            }
        }
    }
    val list91 = buildList {
        add("one")

        with (get(0)) {
            with (Inv(this)) {
                <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
            }
        }
    }

    // Resolution ambiguities below aren't due to stub types
    val list10 = buildList {
        add("one")

        foo0(get(0), 0f)
    }
    val list11 = buildList {
        add("one")

        val x = get(0)
        foo0(x, 0f)
    }
    val list12 = buildList {
        add("one")
        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo00<!>(get(0), 0f)
    }

    // Below are multi-arguments resolution ambiguities
    val list13 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo000<!>(get(0), 0f, get(0))
    }

    val list14 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0000<!>(get(0), 0f, get(0))
    }

    val list17 = buildList l1@ {
        add("one")

        with (get(0)) {
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0003<!>(0f, this@l1.get(0))
        }
    }

    val list18 = buildList {
        add("one")

        get(0).<!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0003<!>(0f, get(0))
    }

    val map1 = buildMap {
        put(1, "one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo11<!>(entries)
    }

    // There aren't specific errors below as casting value arguments doesn't make a resolve successful
    val list15 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0001<!>(get(0), 0f, get(0))
    }

    val list16 = buildList {
        add("one")

        <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo0002<!>(get(0), 0f, get(0))
    }
}

interface Foo<K> {
    fun add(x: K)
    fun get(): K
}

interface Foo2<K, V> {
    fun put(x: K, y: V)
    fun get(): MutableSet<MutableMap.MutableEntry<K, V>>
    fun entries(): MutableSet<MutableMap.MutableEntry<K, V>>
}

fun <L, K, V> twoBuilderLambdas(@BuilderInference block: Foo<L>.() -> Unit, @BuilderInference block2: Foo2<K, V>.() -> Unit) {}

fun test() {
    twoBuilderLambdas(
        {
            add("")
            with (get()) {
                with (listOf(1)) {
                    <!OVERLOAD_RESOLUTION_AMBIGUITY!>bar<!>()
                }
            }
        },
        {
            put(1, "one")
            <!OVERLOAD_RESOLUTION_AMBIGUITY!>foo11<!>(entries())
        }
    )
}
