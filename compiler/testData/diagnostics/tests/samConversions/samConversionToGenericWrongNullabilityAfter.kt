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
        returnNullableString()
    }

    Supplier<StringAlias> {
        returnNullableString()
    }

    Supplier<String> {
        TestValueProvider.getNullableString()
    }

    Supplier<String> {
        val x = 1
        when(x) {
            1 -> returnNullableString()
            else -> ""
        }
    }

    Supplier<String> {
        if (true) return@Supplier returnNullableString()
        run { return@Supplier returnNullableString() }
        <!UNREACHABLE_CODE!>try {
            if (true) return@Supplier returnNullableString()
            2
        } finally {
            Unit
        }<!>
        <!UNREACHABLE_CODE!>""<!>
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
            if (true) return <!TYPE_MISMATCH, TYPE_MISMATCH!>returnNullableString()<!>
            return ""
        }
    )

    Supplier<String>(
        <!TYPE_MISMATCH!>fun(): String? {
            if (true) return returnNullableString()
            return ""
        }<!>
    )

    Supplier<String> {
        if (true) return@Supplier returnNullableString()
        ""
    }

    val sam: Supplier<String> = Supplier {
        <!TYPE_MISMATCH!>returnNullableString()<!>
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
        run {
            returnNullableString()
        }
    }

    Supplier<String> {
        run {
            return@run returnNullableString()
        }
    }

    Supplier<String> {
        run run@ {
            return@run returnNullableString()
        }
    }

    Supplier<String> lambda@ {
        run {
            return@lambda returnNullableString()
        }
    }
}

fun <T: Number> test1(x: T) {
    Supplier<T> {
        x.foo()
    }
}

fun <T> test2(x: T) where T: Any?, T: Comparable<T> {
    Supplier<T> {
        x.foo()
    }
}

fun <T> T.foo(): T? = null!!

fun <T> T.foo2(): T? = null!!

fun test()  {
    Supplier<String> {
        returnNullableString().foo2()
    }
}
