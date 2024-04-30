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

// FILE: TestValueProvider.java
import org.jetbrains.annotations.Nullable;

public class TestValueProvider {
    @Nullable
    static String getNullableString() {
        return null;
    }
}

// FILE: test.kt
import java.util.function.Supplier

inline fun run(fn: () -> Unit) = fn()

typealias StringAlias = String

fun main() {
    Supplier<String> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
    }

    Supplier<StringAlias> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
    }

    Supplier<String> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>TestValueProvider.getNullableString()<!>
    }

    val sam: Supplier<String> = Supplier{
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>TestValueProvider.getNullableString()<!>
    }

    Supplier<String> {
        val x = 1
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>when(x) {
            1 -> returnNullableString()
            else -> ""
        }<!>
    }

    Supplier<String> {
        if (true) return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
        run { return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!> }
        try {
            if (true) return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
            2
        } finally {
            Unit
        }
        ""
    }

    Supplier<String?> {
        returnNullableString()
    }

    val sam2: Supplier<String?> = Supplier {
        returnNullableString()
    }

    Supplier<_> {
        returnNullableString()
    }

    Supplier {
        returnNullableString()
    }

    val sam3: Supplier<String> = Supplier{
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
    }

    Supplier<String>(
        fun(): String {
            if (true) return <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>returnNullableString()<!>
            return ""
        }
    )

    val sam4: Supplier<String> = Supplier {
        <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun(): String {
            if (true) return <!RETURN_TYPE_MISMATCH!>returnNullableString()<!>
            return ""
        }<!>
    }

    Supplier<String>(
        fun(): String? {
            if (true) return <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
            return ""
        }
    )

    val sam5: Supplier<String> = Supplier {
        <!ARGUMENT_TYPE_MISMATCH, ARGUMENT_TYPE_MISMATCH!>fun(): String? {
            if (true) return returnNullableString()
            return ""
        }<!>
    }

    Supplier<String> {
        if (true) return@Supplier <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString()<!>
        ""
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

    val mySam: MySupplier<String> = MySupplier{
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
            return@run <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>returnNullableString()<!>
        }<!>
    }

    Supplier<String> {
        <!ARGUMENT_TYPE_MISMATCH!>run run@ {
            return@run <!ARGUMENT_TYPE_MISMATCH, RETURN_TYPE_MISMATCH!>returnNullableString()<!>
        }<!>
    }

    Supplier<String> lambda@ {
        <!ARGUMENT_TYPE_MISMATCH!>run {
            return@lambda returnNullableString()
        }<!>
    }
}

fun <T: Number> test1(x: T) {
    Supplier<T> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>x.foo()<!>
    }
}

fun <T> test2(x: T) where T: Any?, T: Comparable<T> {
    Supplier<T> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>x.foo()<!>
    }
}

fun <T> T.foo(): T? = null!!

fun <T> T.foo2(): T? = null!!

fun test()  {
    Supplier<String> {
        <!TYPE_MISMATCH_WHEN_FLEXIBILITY_CHANGES!>returnNullableString().foo2()<!>
    }
}
