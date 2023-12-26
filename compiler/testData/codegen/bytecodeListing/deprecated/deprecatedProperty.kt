// JVM_ABI_K1_K2_DIFF: KT-63850, KT-63854

@Deprecated("")
val testVal = 1

@Deprecated("")
var testVar = 1

@Deprecated("")
val testValWithExplicitDefaultGet = 1
    get

@Deprecated("")
val testValWithExplicitGet
    get() = 1

@Deprecated("")
var testVarWithExplicitDefaultGet = 1
    get

@Deprecated("")
var testVarWithExplicitDefaultSet = 1
    set

@Deprecated("")
var testVarWithExplicitDefaultGetSet: Int = 1
    get
    set

@Deprecated("")
var testVarWithExplicitGetSet
    get() = 1
    set(v) {}

@Deprecated("")
lateinit var testLateinitVar: Any

@Deprecated("")
val Any.testExtVal
    get() = 1

@Deprecated("")
var Any.textExtVar
    get() = 1
    set(v) {}

@Deprecated("")
val <T> List<T>.testGenExtVal
    get() = 1

@Deprecated("")
var <T> List<T>.textGenExtVar
    get() = 1
    set(v) {}

interface I {
    @Deprecated("")
    val <T> T.id: T
        get() = this
}
