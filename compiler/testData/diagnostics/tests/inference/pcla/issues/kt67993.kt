// ISSUE: KT-67993

import kotlin.reflect.KProperty

class Klass {
    private val value1 = build {
        object {
            fun bar() { consume(foo()) }
            private fun foo() = ""
        }
    }
    private val value2 = build {
        object {
            fun baz() { consume(bar()) }
            private fun bar() = foo()
            private fun foo() = ""
        }
    }
    private val value3 = build {
        object {
            fun bar() = consume(foo())
            private fun foo() = ""
        }
    }
    private val value4 = build {
        object {
            fun baz() = consume(bar())
            private fun bar() = foo()
            private fun foo() = ""
        }
    }
    private val value5 = build {
        object {
            fun bar() { accept<String>(foo()) }
            private fun foo() = produce()
        }
    }
    private val value6 = build {
        object {
            fun baz() { accept<String>(bar()) }
            private fun bar() = foo()
            private fun foo() = produce()
        }
    }
    private val value7 = build {
        object {
            fun bar() = accept<String>(foo())
            private fun foo() = produce()
        }
    }
    private val value8 = build {
        object {
            fun baz() = accept<String>(bar())
            private fun bar() = foo()
            private fun foo() = produce()
        }
    }

    private val value9 = build {
        class Local {
            fun bar() { consume(foo()) }
            private fun foo() = ""
        }
    }
    private val value10 = build {
        class Local {
            fun bar() { consume(foo()) }
            private fun foo() = this
        }
    }
    private val value11 = build {
        class Local {
            fun foo() { consume(Local()) }
        }
    }

    private val value12 = build {
        class LocalA {
            inner class LocalB {
                fun bar() { consume(foo()) }
            }
            private fun foo() = ""
        }
    }
    private val value13 = build {
        class LocalA {
            inner class LocalB {
                fun bar() { consume(foo()) }
            }
            private fun foo() = this
        }
    }
    private val value14 = build {
        class LocalA {
            inner class LocalB {
                fun foo() { consume(LocalA()) }
            }
        }
    }

    private val value15 = build {
        class LocalA {
            inner class LocalB {
                inner class LocalC {
                    fun bar() { consume(foo()) }
                }
            }
            private fun foo() = ""
        }
    }
    private val value16 = build {
        class LocalA {
            inner class LocalB {
                inner class LocalC {
                    fun bar() { consume(foo()) }
                }
            }
            private fun foo() = this
        }
    }
    private val value17 = build {
        class LocalA {
            inner class LocalB {
                inner class LocalC {
                    fun foo() { consume(LocalA()) }
                }
            }
        }
    }

    private val value18 = build {
        class LocalB {
            fun foo() = ""
        }
        class LocalA {
            fun bar() { consume(LocalB().foo()) }
        }
    }
    private val value19 = build {
        class LocalB {
            fun foo() = this
        }
        class LocalA {
            fun bar() { consume(LocalB().foo()) }
        }
    }
    private val value20 = build {
        class LocalB
        class LocalA {
            fun foo() { consume(LocalB()) }
        }
    }

    private val value21 = build {
        class LocalB {
            inner class LocalC {
                fun foo() = ""
            }
        }
        class LocalA {
            fun bar() { consume(LocalB().LocalC().foo()) }
        }
    }
    private val value22 = build {
        class LocalB {
            inner class LocalC {
                fun foo() = this
            }
        }
        class LocalA {
            fun bar() { consume(LocalB().LocalC().foo()) }
        }
    }
    private val value23 = build {
        class LocalB {
            inner class LocalC {
                fun bar() = foo()
            }
            private fun foo() = ""
        }
        class LocalA {
            fun baz() { consume(LocalB().LocalC().bar()) }
        }
    }
    private val value24 = build {
        class LocalB {
            inner class LocalC {
                fun bar() = foo()
            }
            private fun foo() = this
        }
        class LocalA {
            fun baz() { consume(LocalB().LocalC().bar()) }
        }
    }
    private val value25 = build {
        class LocalB {
            inner class LocalC
        }
        class LocalA {
            fun foo() { consume(LocalB().LocalC()) }
        }
    }

    private val value26 = build {
        class LocalB {
            fun foo() = ""
        }
        class LocalA {
            fun bar() { consume(bInstance.foo()) }
            private val bInstance = LocalB()
        }
    }
    private val value27 = build {
        class LocalB {
            fun foo() = this
        }
        class LocalA {
            fun bar() { consume(bInstance.foo()) }
            private val bInstance = LocalB()
        }
    }
    private val value28 = build {
        class LocalB
        class LocalA {
            fun foo() { consume(bInstance) }
            private val bInstance = LocalB()
        }
    }

    private val value29 = build {
        class LocalB {
            inner class LocalC {
                fun foo() = ""
            }
            val cInstance = LocalC()
        }
        class LocalA {
            fun bar() { consume(bInstance.cInstance.foo()) }
            private val bInstance = LocalB()
        }
    }
    private val value30 = build {
        class LocalB {
            inner class LocalC {
                fun foo() = this
            }
            val cInstance = LocalC()
        }
        class LocalA {
            fun bar() { consume(bInstance.cInstance.foo()) }
            private val bInstance = LocalB()
        }
    }
    private val value31 = build {
        class LocalB {
            inner class LocalC {
                fun bar() = foo()
            }
            private fun foo() = ""
            val cInstance = LocalC()
        }
        class LocalA {
            fun baz() { consume(bInstance.cInstance.bar()) }
            private val bInstance = LocalB()
        }
    }
    private val value32 = build {
        class LocalB {
            inner class LocalC {
                fun bar() = foo()
            }
            private fun foo() = this
            val cInstance = LocalC()
        }
        class LocalA {
            fun baz() { consume(bInstance.cInstance.bar()) }
            private val bInstance = LocalB()
        }
    }
    private val value33 = build {
        class LocalB {
            inner class LocalC
            val cInstance = LocalC()
        }
        class LocalA {
            fun foo() { consume(bInstance.cInstance) }
            private val bInstance = LocalB()
        }
    }

    private val value34 = build {
        class LocalA {
            fun bar() { consume(nestedBInstance.fooB1()) }
        }
    }
    private val value35 = build {
        class LocalA {
            fun bar() { consume(nestedBInstance.fooB2()) }
        }
    }
    private val value36 = build {
        class LocalA {
            fun foo() { consume(NestedB()) }
        }
    }

    private val value37 = build {
        class LocalA {
            fun bar() { consume(nestedBInstance.cInstance.fooC1()) }
        }
    }
    private val value38 = build {
        class LocalA {
            fun bar() { consume(nestedBInstance.cInstance.fooC2()) }
        }
    }
    private val value39 = build {
        class LocalA {
            fun baz() { consume(nestedBInstance.cInstance.barC1()) }
        }
    }
    private val value40 = build {
        class LocalA {
            fun baz() { consume(nestedBInstance.cInstance.barC2()) }
        }
    }
    private val value41 = build {
        class LocalA {
            fun foo() { consume(NestedB().InnerC()) }
        }
    }

    class NestedB {
        fun fooB1() = ""
        fun fooB2() = this
        inner class InnerC {
            fun fooC1() = ""
            fun fooC2() = this
            fun barC1() = fooB1()
            fun barC2() = fooB2()
        }
        val cInstance = InnerC()
    }
    val nestedBInstance = NestedB()

    private val value42 = build {
        class LocalA {
            fun bar() { consume(topLevelBInstance.fooB1()) }
        }
    }
    private val value43 = build {
        class LocalA {
            fun bar() { consume(topLevelBInstance.fooB2()) }
        }
    }
    private val value44 = build {
        class LocalA {
            fun foo() { consume(TopLevelB()) }
        }
    }

    private val value45 = build {
        class LocalA {
            fun bar() { consume(topLevelBInstance.cInstance.fooC1()) }
        }
    }
    private val value46 = build {
        class LocalA {
            fun bar() { consume(topLevelBInstance.cInstance.fooC2()) }
        }
    }
    private val value47 = build {
        class LocalA {
            fun baz() { consume(topLevelBInstance.cInstance.barC1()) }
        }
    }
    private val value48 = build {
        class LocalA {
            fun baz() { consume(topLevelBInstance.cInstance.barC2()) }
        }
    }
    private val value49 = build {
        class LocalA {
            fun foo() { consume(TopLevelB().InnerC()) }
        }
    }

    private val value50 = run {
        class LocalA {
            val value = build {
                class LocalB {
                    fun bar() { consume(foo()) }
                }
            }
            private fun foo() = ""
        }
    }
    private val value51 = run {
        class LocalA {
            val value = build {
                class LocalB {
                    fun bar() { consume(foo()) }
                }
            }
            private fun foo() = this
        }
    }
    private val value52 = run {
        class LocalA {
            val value = build {
                class LocalB {
                    fun foo() { consume(LocalA()) }
                }
            }
        }
    }

    private val value53 = run {
        class LocalA {
            val valueA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> a@{
                class LocalB {
                    val valueB = build b@{
                        class LocalC {
                            fun bar() {
                                this@a.consume(foo())
                                this@b.consume(foo())
                            }
                        }
                    }
                }
            }
            private fun foo() = ""
        }
    }
    private val value54 = run {
        class LocalA {
            val valueA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> a@{
                class LocalB {
                    val valueB = build b@{
                        class LocalC {
                            fun bar() {
                                this@a.consume(foo())
                                this@b.consume(foo())
                            }
                        }
                    }
                }
            }
            private fun foo() = this
        }
    }
    private val value55 = run {
        class LocalA {
            val valueA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> a@{
                class LocalB {
                    val valueB = build b@{
                        class LocalC {
                            fun baz() {
                                this@a.consume(bar())
                                this@b.consume(bar())
                            }
                        }
                    }
                    private fun bar() = foo()
                }
            }
            private fun foo() = ""
        }
    }
    private val value56 = run {
        class LocalA {
            val valueA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> a@{
                class LocalB {
                    val valueB = build b@{
                        class LocalC {
                            fun baz() {
                                this@a.consume(bar())
                                this@b.consume(bar())
                            }
                        }
                    }
                    private fun bar() = foo()
                }
            }
            private fun foo() = this
        }
    }
    private val value57 = run {
        class LocalA {
            val valueA = <!NEW_INFERENCE_NO_INFORMATION_FOR_PARAMETER!>build<!> a@{
                class LocalB {
                    val valueB = build b@{
                        class LocalC {
                            fun foo() {
                                this@a.consume(LocalA())
                                this@b.consume(LocalA())
                            }
                        }
                    }
                }
            }
        }
    }

    private val value58 = build {
        object {
            fun bar() {
                { consume(foo()) }
            }
            private fun foo() = ""
        }
    }
    private val value59 = build {
        object {
            fun bar() = { consume(foo()) }
            private fun foo() = ""
        }
    }
    private val value60 = build {
        object {
            fun bar() {
                fun () = consume(foo())
            }
            private fun foo() = ""
        }
    }
    private val value61 = build {
        object {
            fun bar() = fun () = consume(foo())
            private fun foo() = ""
        }
    }

    private val value62 = build {
        object {
            fun bar() {
                consume((::foo)())
            }
            private fun foo() = ""
        }
    }
    private val value63 = build {
        object {
            fun bar() {
                consumeCallable(::foo)
            }
            private fun foo() = ""
        }
    }
    private val value64 = build {
        object {
            fun bar() {
                accept<String>((::foo)())
            }
            private fun foo() = produce()
        }
    }
    private val value65 = build {
        object {
            fun bar() {
                acceptCallable<String>(::foo)
            }
            private fun foo() = produce()
        }
    }

    private val value66 = build {
        object {
            val bar = consume(foo)
            private val foo get() = ""
        }
    }
    private val value67 = build {
        object {
            val bar get() = consume(foo)
            private val foo get() = ""
        }
    }
    private val value68 = build {
        object {
            var bar
                set(value) = run { consume(foo) }
                get() = null
            private val foo get() = ""
        }
    }
    private val value69 = build {
        object {
            var bar
                set(value) = run(fun () = consume(foo))
                get() = null
            private val foo get() = ""
        }
    }
    private val value70 = build {
        object {
            var bar
                set(value) = run { consume(value) }
                get() = ""
        }
    }
    private val value71 = build {
        object {
            var bar
                set(value) = run(fun () = consume(value))
                get() = ""
        }
    }
    private val value72 = build {
        object {
            var bar = ""
                set(value) = run { consume(field) }
        }
    }
    private val value73 = build {
        object {
            var bar = ""
                set(value) = run(fun () = consume(field))
        }
    }

    private val value74 = build {
        object {
            val bar by Delegate(consume(foo))
            private val foo get() = ""
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value75 = build {
        object {
            val bar by consume(foo)
            private val foo get() = ""
            operator fun Unit.getValue(thisRef: Any?, property: KProperty<*>) = this
        }
    }
    private val value76 = build {
        object {
            val bar by Delegate { consume(foo) }
            private val foo get() = ""
            inner class Delegate<T>(private val materialize: () -> T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = materialize()
            }
        }
    }
    private val value77 = build {
        object {
            val bar by Delegate(fun () = consume(foo))
            private val foo get() = ""
            inner class Delegate<T>(private val materialize: () -> T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = materialize()
            }
        }
    }
    private val value78 = build {
        object {
            val bar by Delegate(processCallable(::foo))
            private val foo get() = ""
            inner class Delegate<T>(private val materialize: () -> T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = materialize()
            }
        }
    }
    private val value79 = build {
        object {
            val bar by processCallable(::foo)
            private val foo get() = ""
            operator fun <T> (() -> T).getValue(thisRef: Any?, property: KProperty<*>) = this()
        }
    }

    private val value80 = build {
        object {
            val bar by DelegateProvider(consume(foo))
            private val foo get() = ""
            inner class DelegateProvider<T>(private val value: T) {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(value)
            }
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value81 = build {
        object {
            val bar by consume(foo)
            private val foo get() = ""
            operator fun Unit.provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(this)
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value82 = build {
        object {
            val bar by DelegateProvider { consume(foo) }
            private val foo get() = ""
            inner class DelegateProvider<T>(private val materialize: () -> T) {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(materialize())
            }
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value83 = build {
        object {
            val bar by DelegateProvider(fun () = consume(foo))
            private val foo get() = ""
            inner class DelegateProvider<T>(private val materialize: () -> T) {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(materialize())
            }
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value84 = build {
        object {
            val bar by DelegateProvider(processCallable(::foo))
            private val foo get() = ""
            inner class DelegateProvider<T>(private val materialize: () -> T) {
                operator fun provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(materialize())
            }
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
    private val value85 = build {
        object {
            val bar by processCallable(::foo)
            private val foo get() = ""
            operator fun <T> (() -> T).provideDelegate(thisRef: Any?, property: KProperty<*>) = Delegate(this())
            inner class Delegate<T>(private val value: T) {
                operator fun getValue(thisRef: Any?, property: KProperty<*>) = value
            }
        }
    }
}

class TopLevelB {
    fun fooB1() = ""
    fun fooB2() = this
    inner class InnerC {
        fun fooC1() = ""
        fun fooC2() = this
        fun barC1() = fooB1()
        fun barC2() = fooB2()
    }
    val cInstance = InnerC()
}
val topLevelBInstance = TopLevelB()

class Buildee<T> {
    fun consume(arg: T) {}
    fun produce(): T = null!!
    fun consumeCallable(arg: () -> T) {}
    fun produceCallable(): () -> T = { null!! }
    fun processCallable(arg: () -> T): () -> T = arg
}

fun <T> build(instructions: Buildee<T>.() -> Unit): Buildee<T> {
    return Buildee<T>().apply(instructions)
}

fun <T> accept(arg: T) {}
fun <T> acceptCallable(arg: () -> T) {}
