// FILE: J.java
import org.jetbrains.annotations.NotNull;

public interface J {
    @NotNull
    public Integer foo();
}

// FILE: test.kt
fun Long.id() = this

fun Short.id() = this

fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun doSimple1(s: String?) = s?.length == 3

fun doJava1(s: String?, j: J) = s?.length == j.foo()

fun doLongReceiver1(x: Long?) = x?.id() == 3L

fun doShortReceiver1(x: Short?, y: Short) = x?.id() == y

fun doChain1(s: String?) = s?.drop2()?.length == 1

fun doIf1(s: String?) =
        if (s?.length == 1) "A" else "B"

fun doSimple2(s: String?) = 3 == s?.length

fun doJava2(s: String?, j: J) = j.foo() == s?.length

fun doLongReceiver2(x: Long?) = 3L == x?.id()

fun doShortReceiver2(x: Short?, y: Short) = y == x?.id()

fun doChain2(s: String?) = 1 == s?.drop2()?.length

fun doIf2(s: String?) =
        if (1 == s?.length) "A" else "B"

// `doJava1`/`doJava2` box `s?.length` instead of unboxing `j.foo()`:
// 2 valueOf
