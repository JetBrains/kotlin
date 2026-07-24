// IGNORE_BACKEND: JS_IR, JS_IR_ES6
// IGNORE_KLIB_RUNTIME_ERRORS_WITH_CUSTOM_SECOND_STAGE: Wasm-js:2.3,2.4
// ISSUE: KT-83356 K/Wasm: Difference in behavior on nested class initialization (for enums?)

var log = ""

fun checkLog(testName: String, expected: String): String? {
    if (log != expected) return "$testName: expected <$expected>, actual <$log>"
    log = ""
    return null
}

open class NestedObjectBase(name: String) {
    init {
        log += "$name.base.init;"
    }
}

enum class DirectEntryAccess(a: String) {
    X("x"),
    Y("y");

    init {
        log += "DirectEntryAccess.init($a);"
    }

    object Nested {
        init {
            log += "DirectEntryAccess.Nested.init;"
        }

        fun entry(): DirectEntryAccess {
            log += "DirectEntryAccess.Nested.entry.before;"
            val result = X
            log += "DirectEntryAccess.Nested.entry.after;"
            return result
        }
    }
}

enum class PropertyInitializerEntryAccess(a: String) {
    X("x"),
    Y("y");

    init {
        log += "PropertyInitializerEntryAccess.init($a);"
    }

    object Nested {
        val entryName = run {
            log += "PropertyInitializerEntryAccess.Nested.entryName;"
            usePropertyInitializerEntry(X)
        }

        init {
            log += "PropertyInitializerEntryAccess.Nested.init;"
        }
    }
}

fun usePropertyInitializerEntry(value: PropertyInitializerEntryAccess): String {
    log += "PropertyInitializerEntryAccess.use;"
    return value.name
}

enum class RegularNestedClass(a: String) {
    X("x"),
    Y("y");

    init {
        log += "RegularNestedClass.init($a);"
    }

    class Nested {
        init {
            log += "RegularNestedClass.Nested.init;"
        }

        fun ping() {
            log += "RegularNestedClass.Nested.ping;"
        }
    }
}

enum class NamedCompanion(a: String) {
    X("x"),
    Y("y");

    init {
        log += "NamedCompanion.init($a);"
    }

    companion object Named {
        init {
            log += "NamedCompanion.Named.init;"
        }

        fun ping() {
            log += "NamedCompanion.Named.ping;"
        }
    }
}

enum class ObjectWithSuperclass(a: String) {
    X("x"),
    Y("y");

    init {
        log += "ObjectWithSuperclass.init($a);"
    }

    object Nested : NestedObjectBase("ObjectWithSuperclass.Nested") {
        init {
            log += "ObjectWithSuperclass.Nested.init;"
        }

        fun ping() {
            log += "ObjectWithSuperclass.Nested.ping;"
        }
    }
}

enum class ObjectInsideNestedClass(a: String) {
    X("x"),
    Y("y");

    init {
        log += "ObjectInsideNestedClass.init($a);"
    }

    class Nested {
        object InsideNested {
            init {
                log += "ObjectInsideNestedClass.Nested.InsideNested.init;"
            }

            fun ping() {
                log += "ObjectInsideNestedClass.Nested.InsideNested.ping;"
            }
        }
    }
}

enum class RepeatedNestedObjectAccess(a: String) {
    X("x"),
    Y("y");

    init {
        log += "RepeatedNestedObjectAccess.init($a);"
    }

    object Nested {
        init {
            log += "RepeatedNestedObjectAccess.Nested.init;"
        }

        fun ping() {
            log += "RepeatedNestedObjectAccess.Nested.ping;"
        }
    }
}

enum class EntryWithBody(a: String) {
    X("x") {
        override fun marker(): String {
            log += "EntryWithBody.X.marker;"
            return "X"
        }
    },
    Y("y");

    init {
        log += "EntryWithBody.init($a);"
    }

    open fun marker(): String {
        log += "EntryWithBody.default.marker;"
        return name
    }

    object Nested {
        init {
            log += "EntryWithBody.Nested.init;"
        }

        fun ping() {
            log += "EntryWithBody.Nested.ping;"
        }
    }
}

fun box(): String {
    val first = DirectEntryAccess.Nested.entry()
    val second = DirectEntryAccess.Nested.entry()
    log += "${first.name};${second.name};"
    var failure = checkLog(
        "nested object explicit enum entry access is initialized once",
        "DirectEntryAccess.Nested.init;DirectEntryAccess.Nested.entry.before;DirectEntryAccess.init(x);DirectEntryAccess.init(y);DirectEntryAccess.Nested.entry.after;DirectEntryAccess.Nested.entry.before;DirectEntryAccess.Nested.entry.after;X;X;"
    )
    if (failure != null) return failure

    val entryName = PropertyInitializerEntryAccess.Nested.entryName
    log += "$entryName;"
    failure = checkLog(
        "nested object property initializer explicit enum entry access",
        "PropertyInitializerEntryAccess.Nested.entryName;PropertyInitializerEntryAccess.init(x);PropertyInitializerEntryAccess.init(y);PropertyInitializerEntryAccess.use;PropertyInitializerEntryAccess.Nested.init;X;"
    )
    if (failure != null) return failure

    val nested = RegularNestedClass.Nested()
    nested.ping()
    failure = checkLog(
        "nested class constructor access",
        "RegularNestedClass.Nested.init;RegularNestedClass.Nested.ping;"
    )
    if (failure != null) return failure

    NamedCompanion.Named.ping()
    failure = checkLog(
        "named companion object access",
        "NamedCompanion.init(x);NamedCompanion.init(y);NamedCompanion.Named.init;NamedCompanion.Named.ping;"
    )
    if (failure != null) return failure

    ObjectWithSuperclass.Nested.ping()
    failure = checkLog(
        "nested object with superclass access",
        "ObjectWithSuperclass.Nested.base.init;ObjectWithSuperclass.Nested.init;ObjectWithSuperclass.Nested.ping;"
    )
    if (failure != null) return failure

    ObjectInsideNestedClass.Nested.InsideNested.ping()
    failure = checkLog(
        "nested object inside nested class access",
        "ObjectInsideNestedClass.Nested.InsideNested.init;ObjectInsideNestedClass.Nested.InsideNested.ping;"
    )
    if (failure != null) return failure

    RepeatedNestedObjectAccess.Nested.ping()
    RepeatedNestedObjectAccess.Nested.ping()
    val repeatedEntry = RepeatedNestedObjectAccess.X
    log += "${repeatedEntry.name};"
    RepeatedNestedObjectAccess.Nested.ping()
    failure = checkLog(
        "repeated nested object access before and after enum entry access",
        "RepeatedNestedObjectAccess.Nested.init;RepeatedNestedObjectAccess.Nested.ping;RepeatedNestedObjectAccess.Nested.ping;RepeatedNestedObjectAccess.init(x);RepeatedNestedObjectAccess.init(y);X;RepeatedNestedObjectAccess.Nested.ping;"
    )
    if (failure != null) return failure

    EntryWithBody.Nested.ping()
    failure = checkLog(
        "nested object access with enum entry anonymous body",
        "EntryWithBody.Nested.init;EntryWithBody.Nested.ping;"
    )
    if (failure != null) return failure

    val marker = EntryWithBody.X.marker()
    log += "$marker;"
    failure = checkLog(
        "enum entry anonymous body access",
        "EntryWithBody.init(x);EntryWithBody.init(y);EntryWithBody.X.marker;X;"
    )
    if (failure != null) return failure

    return "OK"
}
