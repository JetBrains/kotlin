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

// FILE: JavaTestValueProvider.java
import org.jetbrains.annotations.Nullable;

public class TestValueProvider {
    @Nullable
    static String getNullableString() {
        return null;
    }
}

// FILE: test.kt
import java.util.function.Supplier

typealias StringAlias = String

fun main() {
    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
    }

    Supplier<StringAlias> {
        <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
    }

    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>TestValueProvider.getNullableString()<!>
    }

    Supplier<String> {
        val x = 1
        <!ARGUMENT_TYPE_MISMATCH!>when(x) {
            1 -> returnNullableString()
            else -> ""
        }<!>
    }

    Supplier<String> {
        if (true) return@Supplier <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
        run { return@Supplier <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!> }
        try {
            if (true) return@Supplier <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
            2
        } finally {
            Unit
        }
        ""
    }

    Supplier<String?> {
        returnNullableString()
    }

    Supplier<_> {
        returnNullableString()
    }

    Supplier {
        returnNullableString()
    }

    Supplier<String>(
        fun(): String {
            if (true) return <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>returnNullableString()<!>
            return ""
        }
    )

    Supplier<String>(
        <!ARGUMENT_TYPE_MISMATCH!>fun(): String? {
            if (true) return returnNullableString()
            return ""
        }<!>
    )

    Supplier<String> {
        if (true) return@Supplier <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
        ""
    }

    val sam: Supplier<String> = Supplier {
        <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
    }

    object : Supplier<String> {
        override fun get(): <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>String?<!> = returnNullableString()
    }

    object : Supplier<String> {
        override fun <!RETURN_TYPE_MISMATCH_ON_OVERRIDE!>get<!>() = returnNullableString()
    }

    MySupplier<String> {
        returnNullableString()
    }

    object : MySupplier<String> {
        override fun get(): String? = returnNullableString()
    }

    object : MySupplier<String> {
        override fun get() = returnNullableString()
    }

    StringSupplier {
        returnNullableString()
    }

    object : StringSupplier {
        override fun get(): String? = returnNullableString()
    }

    object : StringSupplier {
        override fun get() = returnNullableString()
    }
}

fun returnNullableString(): String? = null

// FILE: edge-cases.kt
import java.util.function.Supplier

fun scopes () {
    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>run {
            returnNullableString()
        }<!>
    }

    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>run {
            return@run returnNullableString()
        }<!>
    }

    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>run run@ {
            return@run returnNullableString()
        }<!>
    }

    Supplier<String> lambda@ {
        run {
            return@lambda <!ARGUMENT_TYPE_MISMATCH!>returnNullableString()<!>
        }
    }
}

fun <T: Number> test1(x: T) {
    Supplier<T> {
        <!ARGUMENT_TYPE_MISMATCH!>x.foo()<!>
    }
}

fun <T> test2(x: T) where T: Any?, T: Comparable<T> {
    Supplier<T> {
        <!ARGUMENT_TYPE_MISMATCH!>x.foo()<!>
    }
}

fun <T> T.foo(): T? = null!!

fun <T> T.foo2(): T? = null!!

fun test()  {
    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>returnNullableString().foo2()<!>
    }
}
