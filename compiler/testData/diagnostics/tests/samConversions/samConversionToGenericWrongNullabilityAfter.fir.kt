// LANGUAGE: +JavaTypeParameterDefaultRepresentationWithDNN
// ISSUE: KT-57014
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

fun main() {
    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>foo()<!>
    }

    Supplier<String>(
        fun(): String {
            if (true) return <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>foo()<!>
            return ""
        }
    )

    Supplier<String>(
        <!ARGUMENT_TYPE_MISMATCH!>fun(): String? {
            if (true) return foo()
            return ""
        }<!>
    )

    Supplier<String> {
        if (true) return@Supplier <!ARGUMENT_TYPE_MISMATCH!>foo()<!>
        ""
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
