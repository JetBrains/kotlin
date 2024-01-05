// !LANGUAGE: +UnrestrictedBuilderInference
// !DIAGNOSTICS: -DEPRECATION -OPT_IN_IS_NOT_ENABLED
// WITH_STDLIB
// MUTE_LL_FIR: KT-64741

// FILE: main.kt
import kotlin.experimental.ExperimentalTypeInference

interface TestInterface<R> {
    fun emit(r: R)
    fun get(): R
}

@OptIn(ExperimentalTypeInference::class)
fun <R1> build(block: TestInterface<R1>.() -> Unit): R1 = TODO()

fun Any.test() {}
fun Any?.test2() {}

fun test() {
    val ret1 = build {
        emit(1)
        emit(null)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test2()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>.test2()
        get()?.hashCode()
        get()?.equals(1)
        // there is `String?.equals` extension
        <!TYPE_MISMATCH("Any; Nothing?")!>get()<!>.equals("")
    }
    val ret2 = build {
        emit(1)
        emit(null)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test2()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>.test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)
        <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.equals("")
    }
    val ret3 = build {
        emit(1)
        emit(null)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test2()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>.test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x != null) {
            x<!UNNECESSARY_SAFE_CALL!>?.<!>hashCode()
            x<!UNNECESSARY_SAFE_CALL!>?.<!>equals(1)
            x.equals("")
            x.hashCode()
            x.toString()
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!>.test()
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!><!UNNECESSARY_SAFE_CALL!>?.<!>test2()
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!>.test2()
        }

        ""
    }
    val ret4 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.hashCode()
        }

        ""
    }
    val ret401 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.equals("")
        }

        ""
    }
    val ret402 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            x.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NONE_APPLICABLE!>toString<!>("")
        }

        ""
    }
    val ret403 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()
        }

        ""
    }
    val ret404 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.hashCode()
        }

        ""
    }
    val ret405 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.equals("")
        }

        ""
    }
    val ret406 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            x.<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE, NONE_APPLICABLE!>toString<!>("")
        }

        ""
    }
    val ret407 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()
        }

        ""
    }
    val ret408 = build {
        emit(1)
        emit(null)
        val x = get()
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()

        ""
    }
    val ret41 = build {
        emit(1)
        emit(null)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test2()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>.test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x == null) {
            <!DEBUG_INFO_CONSTANT!>x<!>?.hashCode()
        }

        if (x == null) {
            <!DEBUG_INFO_CONSTANT!>x<!>?.equals(1)
        }

        if (x == null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, DEBUG_INFO_CONSTANT!>x<!>?.test2()
        }

        if (x == null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!>.test2()
        }

        if (x === null) {
            <!DEBUG_INFO_CONSTANT!>x<!>?.hashCode()
        }

        if (x === null) {
            <!DEBUG_INFO_CONSTANT!>x<!>?.equals(1)
        }

        if (x === null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, DEBUG_INFO_CONSTANT!>x<!>?.test2()
        }

        if (x === null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!>.test2()
        }

        ""
    }
    val ret5 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.equals("")
        }

        ""
    }
    val ret501 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.hashCode()
        }
        ""
    }
    val ret502 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.toString()
        }
        ""
    }
    val ret503 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x == null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()
        }
        ""
    }
    val ret504 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.equals("")
        }

        ""
    }
    val ret505 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.hashCode()
        }
        ""
    }
    val ret506 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!TYPE_MISMATCH("Any; Nothing?")!>x<!>.toString()
        }
        ""
    }
    val ret507 = build {
        emit(1)
        emit(null)
        val x = get()
        if (x === null) {
            <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()
        }
        ""
    }
    val ret508 = build {
        emit(1)
        emit(null)
        val x = get()
        <!BUILDER_INFERENCE_STUB_RECEIVER, TYPE_MISMATCH("Any; Nothing?")!>x<!>.test()
        ""
    }
    val ret51 = build {
        emit(1)
        emit(null)
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>?.test2()
        <!BUILDER_INFERENCE_STUB_RECEIVER!>get()<!>.test2()
        get()?.hashCode()
        get()?.equals(1)
        val x = get()
        x?.hashCode()
        x?.equals(1)

        if (get() == null) {}
        if (get() === null) {}

        if (x == null) {
            <!DEBUG_INFO_CONSTANT!>x<!>?.hashCode()
            <!DEBUG_INFO_CONSTANT!>x<!>?.equals(1)
            <!BUILDER_INFERENCE_STUB_RECEIVER, DEBUG_INFO_CONSTANT!>x<!>?.test2()
            <!BUILDER_INFERENCE_STUB_RECEIVER!>x<!>.test2()
        }

        ""
    }
}
