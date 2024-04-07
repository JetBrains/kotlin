// LANGUAGE: -JavaTypeParameterDefaultRepresentationWithDNN
// RENDER_DIAGNOSTICS_FULL_TEXT
// ISSUE: KT-57014, KT-66730
// FULL_JDK
// JVM_TARGET: 1.8

// FILE: MySupplier.java
public interface MySupplier<T> {
    T get();
}

// FILE: StringSupplier.java
public interface StringSupplier {
    String get();
}

// FILE: test.kt
import java.util.function.Supplier

inline fun run(fn: () -> Unit) = fn()

fun main() {
    Supplier<String> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!>
    }

    Supplier<String>(
        fun(): String {
            if (true) return <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH, TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!>
            return ""
        }
    )

    Supplier<String>(
        fun(): String? {
            if (true) return <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!>
            return ""
        }
    )

    Supplier<String> {
        if (true) return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!>
        run { return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!> }
        try {
            if (true) return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>foo()<!>
            2
        } finally {
            Unit
        }
        ""
    }

    Supplier<String?> {
        foo()
    }

    Supplier<_> {
        foo()
    }

    Supplier {
        foo()
    }

    object : Supplier<String> {
        override fun get(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = foo()
    }

    object : Supplier<String> {
        override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>get<!>() = foo()
    }

    MySupplier<String> {
        foo()
    }

    object : MySupplier<String> {
        override fun get(): String? = foo()
    }

    object : MySupplier<String> {
        override fun get() = foo()
    }

    StringSupplier {
        foo()
    }

    object : StringSupplier {
        override fun get(): String? = foo()
    }

    object : StringSupplier {
        override fun get() = foo()
    }
}

fun foo(): String? = null
